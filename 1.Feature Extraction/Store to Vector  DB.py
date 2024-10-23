from pymilvus import connections, FieldSchema, CollectionSchema, DataType, Collection
from sentence_transformers import SentenceTransformer
import numpy as np
import Javalong_Test

# 连接到 Milvus 实例
connections.connect("default", host="localhost", port="19530")

# 创建字段模式
fields = [
    FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
    FieldSchema(name="vector", dtype=DataType.FLOAT_VECTOR, dim=384),  # 维度与使用的嵌入模型一致
    FieldSchema(name="metadata", dtype=DataType.JSON)  # 存储元数据
]

# 创建集合模式
schema = CollectionSchema(fields, description="Java code and metadata collection")

# 创建集合
collection = Collection("java_code_collection", schema)



# 加载 Sentence-BERT 模型
model = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")
# 提取信息并转化为向量和元数据
def convert_code_info_to_vectors_with_metadata(code_info, java_code):
    vectors = []
    metadata = []
    
    for cls in code_info['classes']:
        class_metadata = {
            'class_name': cls['class_name'],
            'fields': cls['fields'],
            'methods': cls['methods'],
            'constructors': cls['constructors'],
            'extends': cls['extends'],
            'implements': cls['implements'],
            'code_snippet': java_code  # 可以存储完整代码或代码片段
        }

        class_text = f"Class: {cls['class_name']}"
        vectors.append(model.encode(class_text).tolist())  # 将向量转换为 list 格式以便插入
        metadata.append(class_metadata)

        for field in cls['fields']:
            field_text = f"Field: {field['field_type']} {field['field_name']}"
            vectors.append(model.encode(field_text).tolist())
            metadata.append({
                'field_type': field['field_type'],
                'field_name': field['field_name'],
                'class_name': cls['class_name'],
                'code_snippet': java_code
            })

        for method in cls['methods']:
            method_params = ", ".join([f"{ptype} {pname}" for ptype, pname in method['parameters']])
            method_text = f"Method: {method['return_type']} {method['method_name']}({method_params})"
            vectors.append(model.encode(method_text).tolist())
            metadata.append({
                'method_name': method['method_name'],
                'parameters': method['parameters'],
                'return_type': method['return_type'],
                'class_name': cls['class_name'],
                'code_snippet': java_code
            })
            
    return vectors, metadata


# 提取 Java 文件信息
# 使用示例
file_path = r"E:\Android RAG\Code\1.Vector DB\1.Feature Extraction\zitmo_banker_reversed\src\main\java\com\android\security\DataStorage.java"
java_code = Javalong_Test.read_java_file(file_path)
code_info = Javalong_Test.extract_code_info(java_code)

# 转化为向量和元数据
vectors, metadata = convert_code_info_to_vectors_with_metadata(code_info, java_code)

# 为集合创建索引
index_params = {
    "metric_type": "L2",  # 选择距离度量方式，可以是 L2 或 IP（内积）
    "index_type": "IVF_FLAT",  # 索引类型
    "params": {"nlist": 128}   # nlist 为分桶数量，越大性能越好，但也消耗更多内存
}

# 创建索引
collection.create_index(field_name="vector", index_params=index_params)

# 插入向量和元数据到 Milvus
entities = [
    vectors,
    metadata
]

collection.insert([entities[0], entities[1]])  # 插入数据
collection.load()  # 加载集合


class_name_to_search = "DataStorage"  # 替换为你想查询的类名

# 执行查询
results = collection.query(
    expr=f"metadata['class_name'] == '{class_name_to_search}'",  # 使用类名进行查询
    output_fields=["metadata", "vector"]  # 返回元数据和向量
)

# 打印查询结果
for result in results:
    metadata = result['metadata']
    java_code = metadata['code_snippet']  # 从元数据中提取原始 Java 代码
    print(f"Class Name: {metadata['class_name']}")
    print(f"Original Java Code: {java_code}")
    print(f"Other Metadata: {metadata}")  # 打印其他元数据信息


# # 检索示例
# query_text = "OpenHelper"
# query_vector = model.encode([query_text]).tolist()

# # 进行检索
# search_params = {"metric_type": "L2", "params": {"nprobe": 10}}
# collection.load()

# # 检索前 5 个最相似的结果
# results = collection.search(
#     data=[query_vector], 
#     anns_field="vector", 
#     param=search_params, 
#     limit=5, 
#     output_fields=["metadata"]
# )

# # 输出检索结果
# for result in results:
#     for match in result:
#         print(f"Score: {match.score}")
#         print(f"Metadata: {match.entity.get('metadata')}")