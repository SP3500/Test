# Load model directly
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM

tokenizer = AutoTokenizer.from_pretrained("Salesforce/codet5-small")
model = AutoModelForSeq2SeqLM.from_pretrained("Salesforce/codet5-small")

# 输入一个自然语言任务描述，模型将尝试生成相应的代码
input_text = "Translate the following function from Python to Java: def add(a, b): return a + b"
input_ids = tokenizer(input_text, return_tensors="pt").input_ids

# 模型生成输出，增加生成最大长度和启用采样
output_ids = model.generate(input_ids, max_length=200, num_beams=5, do_sample=True, top_k=50, top_p=0.95)
output_text = tokenizer.decode(output_ids[0], skip_special_tokens=True)

print("Generated Code:")
print(output_text)