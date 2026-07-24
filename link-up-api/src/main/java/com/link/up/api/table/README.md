## 数据流转模型

Link-Up 通过统一的数据模型，将不同数据源和目标端进行解耦。

```
任意 Source
     |
     ↓
 FluxRecord
     |
     ↓
任意 Sink
```

其中：

* Source：负责读取不同来源的数据（如 MySQL、文件等）
* FluxRecord：Link-Up 内部统一数据结构，包含数据内容和字段类型信息
* Sink：负责将数据写入不同目标端（如数据库、文件等）

通过中间层 FluxRecord，实现数据源和目标端之间的灵活扩展。

