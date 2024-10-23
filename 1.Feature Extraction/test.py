import javalang
from pymilvus import connections, Collection, FieldSchema, CollectionSchema, DataType,utility,Index
from sentence_transformers import SentenceTransformer


# 连接到 Milvus
def connect_milvus():
    connections.connect(host='localhost', port='19530')

def delete_existing_collection(collection_name):
    if utility.has_collection(collection_name):
        utility.drop_collection(collection_name)

# 创建 Milvus Collection
def create_milvus_collection():
    delete_existing_collection("java_class_code")
    fields = [
        FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
        FieldSchema(name="class_name", dtype=DataType.VARCHAR, max_length=255),
        FieldSchema(name="class_code_vector", dtype=DataType.FLOAT_VECTOR, dim=384),
        FieldSchema(name="metadata", dtype=DataType.JSON)
    ]
    schema = CollectionSchema(fields, description="Java class code and metadata")
    collection = Collection("java_class_code", schema)
    return collection

# 提取类的完整代码
def extract_class_code(java_code, node):
    lines = java_code.splitlines()
    class_code = "\n".join(lines[node.position.line-1:])
    return class_code

# 读取Java文件
def read_java_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as file:
        return file.read()


# 提取类名、方法名、字段、构造函数等
def extract_code_info(java_code):
    code_info = []
    
    tree = javalang.parse.parse(java_code)
    
    for path, node in tree:
        if isinstance(node, javalang.tree.ClassDeclaration):
            class_info = {
                'class_name': node.name,
                'fields': [],
                'methods': [],
                'constructors': [],
                'implements': [],
                'extends': None,
                'class_code': extract_class_code(java_code, node)
            }

            if node.extends:
                class_info['extends'] = node.extends.name
            
            if node.implements:
                class_info['implements'] = [impl.name for impl in node.implements]
            
            for member in node.body:
                if isinstance(member, javalang.tree.FieldDeclaration):
                    for declarator in member.declarators:
                        class_info['fields'].append({
                            'field_name': declarator.name,
                            'field_type': member.type.name
                        })
                if isinstance(member, javalang.tree.MethodDeclaration):
                    method_info = {
                        'method_name': member.name,
                        'return_type': member.return_type.name if member.return_type else 'void',
                        'parameters': [(param.type.name, param.name) for param in member.parameters],
                        'local_variables': []
                    }
                    for subpath, subnode in member:
                        if isinstance(subnode, javalang.tree.LocalVariableDeclaration):
                            for declarator in subnode.declarators:
                                method_info['local_variables'].append({
                                    'var_name': declarator.name,
                                    'var_type': subnode.type.name
                                })
                    class_info['methods'].append(method_info)
                if isinstance(member, javalang.tree.ConstructorDeclaration):
                    constructor_info = {
                        'constructor_name': member.name,
                        'parameters': [(param.type.name, param.name) for param in member.parameters]
                    }
                    class_info['constructors'].append(constructor_info)

            # 检查是否重复提取
            if any(info['class_name'] == class_info['class_name'] for info in code_info):
                print(f"Duplicate class found: {class_info['class_name']}")


            code_info.append(class_info)

    return code_info

# 将类的完整代码转换为向量
def generate_code_vector(code_text):
    model = SentenceTransformer('all-MiniLM-L6-v2')
    return model.encode(code_text)


def store_in_milvus(collection, code_info):
    ids = []  # 由于 auto_id=True，因此不需要手动添加 id
    class_names = []
    class_code_vectors = []
    metadatas = []
    
    for class_info in code_info:
        class_code_vector = generate_code_vector(class_info['class_code'])
        metadata = {
            "class_name": class_info['class_name'],
            "extends": class_info['extends'],
            "implements": class_info['implements'],
            "fields": class_info['fields'],
            "methods": class_info['methods'],
            "constructors": class_info['constructors'],
            "class_code": class_info['class_code']  # 保存完整的类代码
        }

        class_names.append(class_info['class_name'])
        class_code_vectors.append(class_code_vector)
        metadatas.append(metadata)

    # 插入到 Milvus 中，每个字段对应一列数据
    collection.insert([class_names, class_code_vectors, metadatas])

# 主流程
def main(file_path):
    connect_milvus()
    collection = create_milvus_collection()
    java_code = read_java_file(file_path)
    code_info = extract_code_info(java_code)
    store_in_milvus(collection, code_info)
    print("Data stored in Milvus successfully.")

# 使用示例
file_path = r"E:\Android RAG\Code\1.Vector DB\1.Feature Extraction\zitmo_banker_reversed\src\main\java\com\android\security\DataStorage.java"
main(file_path)

# 创建索引函数
def create_index(collection):
    index_params = {
        "index_type": "IVF_FLAT",  # 可以根据需求选择不同的索引类型
        "metric_type": "L2",       # L2 是欧几里得距离, 也可以选择 "IP" (内积)
        "params": {"nlist": 128}   # nlist 是聚类中心的数量，参数可以调整
    }
    index = Index(collection, "class_code_vector", index_params)
    print(f"Index created: {index}")

# 加载集合并创建索引
def load_and_index_collection(collection):
    # 首先创建索引
    create_index(collection)
    
    # 然后加载集合到内存中
    collection.load()

# # 查询类名对应的元数据，包括类的完整代码
# def query_by_class_name(collection, class_name):
#     # 确保加载集合
#     collection.load()
    
#     # 查询整个 metadata 字段
#     query_result = collection.query(
#         expr=f'class_name == "{class_name}"',
#         output_fields=["class_name", "metadata"]  # 查询整个 metadata 字段
#     )
    
#     # 提取 metadata 中的 class_code
#     for result in query_result:
#         class_code = result['metadata'].get('class_code', 'Class code not found')
#         print(f"Class Name: {result['class_name']}")
#         print(f"Class Code: {class_code}")
    
#     return query_result

def query_by_class_name(collection, class_name):
    collection.load()
    
    query_result = collection.query(
        expr=f'class_name == "{class_name}"',
        output_fields=["class_name", "metadata"]
    )
    
    # 检查是否有重复内容
    unique_results = set()
    for result in query_result:
        metadata = result['metadata']
        class_code = metadata.get('class_code', 'Class code not found')
        if class_code in unique_results:
            print(f"Duplicate entry found for class: {class_name}")
        else:
            unique_results.add(class_code)
        print(f"Class Name: {result['class_name']}")
        print(f"Class Code: {class_code}")
    
    return query_result

# 示例使用
collection = Collection("java_class_code")
load_and_index_collection(collection)

class_name_to_query = "DataStorage"
result = query_by_class_name(collection, class_name_to_query)

# 输出查询结果
for item in result:
    print(f"Class Name: {item['class_name']}")
    print(f"Class Code:\n{item['metadata']['class_code']}")
