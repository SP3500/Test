from transformers import T5Tokenizer, T5ForConditionalGeneration
from chromadb import Client
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM

# 加载预训练的CodeT5模型和分词器
tokenizer = AutoTokenizer.from_pretrained("Salesforce/codet5-base")
model = AutoModelForSeq2SeqLM.from_pretrained("Salesforce/codet5-base")

# 读取Java文件内容
def read_java_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as file:
        return file.read()

# 使用CodeT5生成代码摘要
def generate_code_summary(code_text):
    # 将代码转换为模型输入格式
    inputs = tokenizer.encode("summarize: " + code_text, return_tensors="pt", max_length=512, truncation=True)
    
    # 生成摘要
    summary_ids = model.generate(inputs, max_length=150, min_length=30, length_penalty=2.0, num_beams=4, early_stopping=True)
    
    # 解码并返回摘要
    summary = tokenizer.decode(summary_ids[0], skip_special_tokens=True)
    return summary

# 文件路径
file_path = r"E:\Android RAG\Samples\Samples\Reversed\zitmo_banker_reversed\src\main\java\com\android\security\DataStorage.java"

# 读取文件并生成摘要
code_text = read_java_file(file_path)
summary = generate_code_summary(code_text)

# 输出代码摘要
print("Generated Code Summary:")
print(summary)
