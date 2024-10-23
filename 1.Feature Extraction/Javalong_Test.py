import javalang

# 读取Java文件
def read_java_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as file:
        return file.read()

# 提取类名、方法名、字段、构造函数等
def extract_code_info(java_code):
    code_info = {
        'package': None,
        'imports': [],
        'classes': []
    }
    
    tree = javalang.parse.parse(java_code)

    # 提取包名
    for path, node in tree:
        if isinstance(node, javalang.tree.PackageDeclaration):
            code_info['package'] = node.name

    # 提取导入的库
    for path, node in tree:
        if isinstance(node, javalang.tree.Import):
            code_info['imports'].append(node.path)
    
    # 提取类名、字段、方法、构造函数等信息
    for path, node in tree:
        if isinstance(node, javalang.tree.ClassDeclaration):
            class_info = {
                'class_name': node.name,
                'fields': [],
                'methods': [],
                'constructors': [],
                'implements': [],
                'extends': None
            }

            # 继承的父类
            if node.extends:
                class_info['extends'] = node.extends.name
            
            # 实现的接口
            if node.implements:
                class_info['implements'] = [impl.name for impl in node.implements]
            
            # 提取字段
            for member in node.body:
                if isinstance(member, javalang.tree.FieldDeclaration):
                    for declarator in member.declarators:
                        class_info['fields'].append({
                            'field_name': declarator.name,
                            'field_type': member.type.name
                        })

                # 提取方法
                if isinstance(member, javalang.tree.MethodDeclaration):
                    method_info = {
                        'method_name': member.name,
                        'return_type': member.return_type.name if member.return_type else 'void',
                        'parameters': [(param.type.name, param.name) for param in member.parameters],
                        'local_variables': []
                    }

                    # 提取局部变量
                    for subpath, subnode in member:
                        if isinstance(subnode, javalang.tree.LocalVariableDeclaration):
                            for declarator in subnode.declarators:
                                method_info['local_variables'].append({
                                    'var_name': declarator.name,
                                    'var_type': subnode.type.name
                                })

                    class_info['methods'].append(method_info)

                # 提取构造函数
                if isinstance(member, javalang.tree.ConstructorDeclaration):
                    constructor_info = {
                        'constructor_name': member.name,
                        'parameters': [(param.type.name, param.name) for param in member.parameters]
                    }
                    class_info['constructors'].append(constructor_info)

            code_info['classes'].append(class_info)

    return code_info

# 使用示例
file_path = r"E:\Android RAG\Code\1.Vector DB\1.Feature Extraction\zitmo_banker_reversed\src\main\java\com\android\security\DataStorage.java"
java_code = read_java_file(file_path)
code_info = extract_code_info(java_code)

# 输出结果
print(f"Package: {code_info['package']}")
print(f"Imports: {', '.join(code_info['imports'])}")

for cls in code_info['classes']:
    print(f"\nClass: {cls['class_name']}")
    if cls['extends']:
        print(f"  Extends: {cls['extends']}")
    if cls['implements']:
        print(f"  Implements: {', '.join(cls['implements'])}")
    
    print("  Fields:")
    for field in cls['fields']:
        print(f"    {field['field_type']} {field['field_name']}")
    
    print("  Methods:")
    for method in cls['methods']:
        params = ", ".join([f"{ptype} {pname}" for ptype, pname in method['parameters']])
        print(f"    {method['return_type']} {method['method_name']}({params})")
        if method['local_variables']:
            print("    Local Variables:")
            for var in method['local_variables']:
                print(f"      {var['var_type']} {var['var_name']}")
    
    print("  Constructors:")
    for constructor in cls['constructors']:
        params = ", ".join([f"{ptype} {pname}" for ptype, pname in constructor['parameters']])
        print(f"    {constructor['constructor_name']}({params})")
