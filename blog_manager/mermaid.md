classDiagram
    %% 控制器层
    class BlogController {
        -BlogFileService blogFileService
        -List~Blog~ blogList
        +listBlogFilenames() Message
        +returnBlogByPath(String) Message
        +updateBlogInfo(Blog) Message
        +updateBlogContent(Blog) Message
        +addBlog(Blog) Message
        +deleteBlog(String) Message
    }
    
    class LLMController {
        -FactoryInterface factory
        -ConfigService configService
        +streamGenerate(String) ResponseEntity
        +getPrompt(String) Message
    }
    
    class ConfigController {
        -ConfigService configService
        +getConfig() Message
        +updateConfig(Config) Message
    }
    
    class ImageController {
        -Path storageLocation
        +uploadImage(MultipartFile) Message
        +listImages() Message
    }
    
    %% 服务层
    class BlogFileService {
        -Path storageLocation
        -Config config
        +listPostFilenames() List~Blog~
        +savePost(Blog) void
        +deleteBlog(Blog) void
        +resolve(String) Path
        +init() void
    }
    
    class ConfigService {
        -Config config
        -String configPath
        +loadConfig() Config
        +saveConfig(Config) void
        +getConfig() Config
        +updateConfig(Config) void
    }
    
    %% 模型层
    class Blog {
        -Path filepath
        -String filename
        -String title
        -LocalDateTime dateTime
        -String categories
        -String tags
        -String saying
        -String content
        +loadContent() String
        +clone() Blog
        +compareTo(Blog) int
        +equals(Object) boolean
    }
    
    class Config {
        -String blogStoragePath
        -String imageStoragePath
        -String xmodelAPIKey
        -String bigmodelAPIKey
        +getBlogStoragePath() String
        +getImageStoragePath() String
    }
    
    class Message {
        -int status
        -Object data
        -String error
        +getStatus() int
        +getData() Object
        +getError() String
    }
    
    %% LLM相关类
    class LLM {
        <<abstract>>
        -String APIKey
        -String APIUrl
        -String model
        #ArrayList~POSTMessage~ messagesArray
        -Boolean IsStream
        -String temperature
        +callLLM(StreamCallback) void
        +getAPIKey() String
        +getAPIUrl() String
    }
    
    class BigModel {
        +callLLM(StreamCallback) void
        +sendRequest(String) String
    }
    
    class XModel {
        +callLLM(StreamCallback) void
        +sendRequest(String) String
    }
    
    class FactoryInterface {
        <<interface>>
        +createLLM() LLM
    }
    
    class BigModelFactory {
        +createLLM() LLM
    }
    
    class XModelFactory {
        +createLLM() LLM
    }
    
    class StreamCallback {
        <<interface>>
        +onNext(String) void
        +onError(Throwable) void
        +onComplete() void
    }
    
    class POSTMessage {
        -String role
        -String content
        +getRole() String
        +getContent() String
    }
    
    %% 关系定义
    BlogController --> BlogFileService : uses
    BlogController --> Blog : manages
    BlogController --> Message : returns
    BlogFileService --> Blog : creates
    BlogFileService --> Config : uses
    
    LLMController --> FactoryInterface : uses
    LLMController --> ConfigService : uses
    LLMController --> Message : returns
    
    ConfigController --> ConfigService : uses
    ConfigController --> Config : manages
    ConfigController --> Message : returns
    
    ConfigService --> Config : manages
    
    FactoryInterface <|.. BigModelFactory : implements
    FactoryInterface <|.. XModelFactory : implements
    
    LLM <|-- BigModel : extends
    LLM <|-- XModel : extends
    
    BigModelFactory --> BigModel : creates
    XModelFactory --> XModel : creates
    
    BigModel --> StreamCallback : uses
    XModel --> StreamCallback : uses
    BigModel --> POSTMessage : uses
    XModel --> POSTMessage : uses
    Blog ..|> Comparable : implements
    Blog ..|> Cloneable : implements
    
    %% 样式定义
    classDef controllerClass fill:#e3f2fd,stroke:#74b9ff
    classDef serviceClass fill:#f0f7ff,stroke:#a29bfe
    classDef modelClass fill:#e8f5ff,stroke:#6c5ce7    
    classDef interfaceClass fill:#f8f9fa,stroke:#b2bec3
    classDef abstractClass fill:#fff3cd,stroke:#ffc107
    
    class BlogController,LLMController,ConfigController,ImageController controllerClass
    class BlogFileService,ConfigService serviceClass
    class Blog,Config,Message,BigModel,XModel,POSTMessage modelClass
    class FactoryInterface,StreamCallback interfaceClass
    class LLM abstractClass