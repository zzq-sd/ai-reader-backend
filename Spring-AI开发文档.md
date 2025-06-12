好的，这是转换为更加鲜明易读的 Markdown 格式的版本：

# Spring AI 1.0 正式发布！核心内容和智能体详解

在经历了八个里程碑式的版本之后（M1~M8），**Spring AI 1.0 正式版本，终于在 2025 年 5 月 20 日正式发布了**！这是另一个新高度的里程碑式的版本，标志着 Spring 生态系统正式全面拥抱人工智能技术，并且意味着 Spring AI 将会给企业带来稳定 API 支持。

## 1. 核心特性

Spring AI 1.0 的核心是 **`ChatClient`** 接口，这是一个可移植且易于使用的 API，是与 AI 模型交互的主要接口。

它支持调用 **20 多种 AI 模型**，从 Anthropic 到 ZhiPu AI，并支持**多模态输入和输出**（当底层模型支持时）以及**结构化响应**（通常以 JSON 格式，便于应用程序处理输出）。

### 1.1 单模型 `ChatClient` 使用

在项目中只有一个模型时，创建全局的 `ChatClient`：

```java
@RestController
class MyController {

    private final ChatClient chatClient;

    public MyController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/ai")
    String generation(String userInput) {
        return this.chatClient.prompt()
            .user(userInput)
            .call()
            .content();
    }
}
```

### 1.2 多模型 `ChatClient` 使用

在项目中有多个模型时，可以为特定的模型创建 `ChatClient`：

```java
// Create ChatClient instances programmatically
ChatModel myChatModel = ... // already autoconfigured by Spring Boot
ChatClient chatClient = ChatClient.create(myChatModel);

// Or use the builder for more control
ChatClient.Builder builder = ChatClient.builder(myChatModel);
ChatClient customChatClient = builder
    .defaultSystemPrompt("You are a helpful assistant.")
    .build();
```

### 1.3 不同模型类型的 `ChatClients`

当项目中有多个模型时，为每个模型定义单独的 `ChatClient`：

```java
import org.springframework.ai.chat.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient openAiChatClient(OpenAiChatModel chatModel) {
        return ChatClient.create(chatModel);
    }

    @Bean
    public ChatClient anthropicChatClient(AnthropicChatModel chatModel) {
        return ChatClient.create(chatModel);
    }
}
```

然后，您可以使用 `@Qualifier` 指定大模型对应的 `ChatClient`：

```java
import org.springframework.ai.chat.client.ChatClient; // 假设的引入，原文未明确指明ChatClient的具体包
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Scanner; // 引入Scanner

@Configuration
public class ChatClientExample {

    @Bean
    CommandLineRunner cli(
            @Qualifier("openAiChatClient") ChatClient openAiChatClient,
            @Qualifier("anthropicChatClient") ChatClient anthropicChatClient) {

        return args -> {
            var scanner = new Scanner(System.in);
            ChatClient chat;

            // Model selection
            System.out.println("\nSelect your AI model:");
            System.out.println("1. OpenAI");
            System.out.println("2. Anthropic");
            System.out.print("Enter your choice (1 or 2): ");

            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                chat = openAiChatClient;
                System.out.println("Using OpenAI model");
            } else { // 简化处理，非1即2
                chat = anthropicChatClient;
                System.out.println("Using Anthropic model");
            }

            // Use the selected chat client
            System.out.print("\nEnter your question: ");
            String input = scanner.nextLine();
            // 原文是 chat.prompt(input).call().content()，为了符合 ChatClient API，可能需要构建 Prompt 对象
            // 这里我们假设 prompt(String) 是一个便捷方法，或者用户会根据实际API调整
            String response = chat.prompt().user(input).call().content();
            System.out.println("ASSISTANT: " + response);

            scanner.close();
        };
    }
}
```

## 2. 主要功能亮点

* **检索增强生成 (RAG)**：Spring AI 提供了便携式向量存储抽象，支持 20 种不同的向量数据库（如 Azure Cosmos DB, Weaviate, Cassandra, PostgreSQL/PGVector, MongoDB Atlas, Milvus, Pinecone, Redis 等）。还包括一个轻量级、可配置的 ETL 框架，用于将数据导入向量存储。
* **对话记忆**：通过 `ChatMemory` 接口管理消息的存储和检索，支持 JDBC、Cassandra 和 Neo4j 等持久化存储。
* **工具调用**：通过 `@Tool` 注解可以轻松定义工具，让 AI 模型能够获取外部信息或执行实际动作。
* **评估与测试**：提供 `Evaluator` 接口和内置的 `RelevancyEvaluator`、`FactCheckingEvaluator`，帮助开发者评估 AI 生成内容的准确性和相关性。
* **可观测性**：与 Micrometer 集成，提供模型延迟、令牌使用情况等关键指标的详细遥测数据。

## 3. 模型上下文协议 (MCP) 支持

Spring AI 1.0 全面支持 **Model Context Protocol (MCP)**，这是一个标准化协议，使 AI 模型能够与外部工具、提示和资源进行交互。Spring AI 提供了客户端和服务器端的 MCP支持，简化了 MCP 工具的使用和创建。

**最简单的 MCP 自定义服务器端实现：**

```java
import org.springframework.ai.model.function.ToolCallbackProvider; // 假设的引入
import org.springframework.ai.model.function.MethodToolCallbackProvider; // 假设的引入
import org.springframework.ai.tool.Tool; // 假设的引入
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.slf4j.Logger; // 引入Logger
import org.slf4j.LoggerFactory; // 引入LoggerFactory


@Service
public class WeatherService {

    @Tool(description = "Get weather information by city name")
    public String getWeather(String cityName) {
        // 伪代码
        return "The weather in " + cityName + " is 21°C and sunny.";
    }
}

@SpringBootApplication
public class McpServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(McpServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider weatherTools(WeatherService weatherService) {
        return MethodToolCallbackProvider.builder().toolObjects(weatherService).build();
    }
}
```

**最简单的 MCP 客户端核心代码实现：**

```java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClientController {
    @Autowired
    private ChatClient chatClient; // 假设 ChatClient 已通过配置注入

    @RequestMapping("/chat")
    public String chat(@RequestParam(value = "msg",defaultValue = "今天天气如何？") String msg) {
        String response = chatClient.prompt()
        .user(msg)
        .call()
        .content();
        System.out.println("响应结果: " + response);
        return response;
    }
}
```

## 4. AI Agent (智能体) 支持

> AI Agent 的核心是“利用 AI 模型与其环境交互，以解决用户定义的任务”。

有效的 AI Agent 将规划、记忆和工具相结合，以完成用户分配的任务。

Spring AI 1.0 支持两种主要类型的 Agent：

* **工作流驱动代理 (Workflow-driven Agents)**：通过预定义路径编排 LLM 和工具，一种更可控的 Agents 实现方法，其中 LLM 和工具通过预定义的路径进行编排。这些工作流是规范性的，可指导 AI 完成既定的操作序列以实现可预测的结果。
* **自主驱动代理 (Autonomous Agents)**：允许 LLM 自主规划和执行处理步骤。这种方式代理将自己决定要调用的路径，决定使用哪些工具以及以什么顺序使用。

虽然完全自主代理的灵活性很有吸引力，但工作流为定义明确的任务提供了更好的可预测性和一致性。具体使用哪种类型，取决于您的具体要求和风险承受能力。

让我们看看 Spring AI 如何通过五种基本模式来实现这些概念，每种模式都服务于特定的用例：

### 4.1 Chain 工作流模式

该模式将复杂任务分解为一系列步骤，其中每个 LLM 调用都会处理前一个 LLM 调用的输出。

*示意图描述：Chain Workflow 模式体现了将复杂任务分解为更简单、更易于管理的步骤的原则。*

#### 使用场景
* 具有明确顺序步骤的任务。
* 当您想用延迟换取更高的准确性时。
* 当每个步骤都基于上一步的输出时。

#### 简单代码实现：
```java
// 假设 ChatClient 已初始化
// public class ChainWorkflow { ... } // 定义包裹类
public class ChainWorkflow {
    private final ChatClient chatClient;
    private final String[] systemPrompts;

    public ChainWorkflow(ChatClient chatClient, String[] systemPrompts) { // 构造函数注入
        this.chatClient = chatClient;
        this.systemPrompts = systemPrompts;
    }

    public String chain(String userInput) {
        String response = userInput;
        for (String prompt : systemPrompts) {
            // 原文格式 "{%s}\n {%s}" 可能需要调整为标准的 String.format 或直接拼接
            String input = String.format("%s\n%s", prompt, response); // 调整了格式化字符串
            response = chatClient.prompt().user(input).call().content(); // 假设 user() 接受完整输入
        }
        return response;
    }
}
```
此实现演示了几个关键原则：
* 每个步骤都有重点。
* 一个步骤的输出成为下一个步骤的输入。
* 该链易于扩展和维护。

### 4.2 并行化工作流

LLM 可以同时处理任务，并以编程方式聚合其输出。

*示意图描述：LLM 可以同时处理多个独立任务，然后汇总结果。*

#### 使用场景
* 处理大量相似但独立的项目。
* 需要多个独立视角的任务。
* 当处理时间至关重要且任务可并行化时。

#### 简单代码实现：
```java
import java.util.List; // 引入 List
// 假设 ChatClient 和 ParallelizationWorkflow 类已定义和初始化
// public class ParallelizationWorkflow { ... }
// List<String> parallelResponse = new ParallelizationWorkflow(chatClient)
// .parallel(
//     "Analyze how market changes will impact this stakeholder group.",
//     List.of(
//         "Customers: ...",
//         "Employees: ...",
//         "Investors: ...",
//         "Suppliers: ..."
//     ),
//     4 // 假设是并行度
// );
```
*注：上述代码为示意，`ParallelizationWorkflow` 的具体实现未在原文中提供。*

### 4.3 路由工作流

路由模式实现了智能任务分配，从而支持对不同类型的输入进行专门处理。

*示意图描述：根据输入内容，将其路由到专门处理该类型任务的 LLM 或流程。*

#### 使用场景
* 具有不同输入类别的复杂任务。
* 当不同的输入需要专门处理时。
* 何时可以准确处理分类。

#### 简单代码实现：
```java
import org.springframework.beans.factory.annotation.Autowired; // 引入 Autowired
import java.util.Map; // 引入 Map
// 假设 ChatClient 和 RoutingWorkflow 类已定义和初始化
// @Autowired // 假设通过Spring注入
// private ChatClient chatClient;

// RoutingWorkflow workflow = new RoutingWorkflow(chatClient); // 假设构造

// Map<String, String> routes = Map.of(
//     "billing", "You are a billing specialist. Help resolve billing issues...",
//     "technical", "You are a technical support engineer. Help solve technical problems...",
//     "general", "You are a customer service representative. Help with general inquiries..."
// );

// String input = "My account was charged twice last week";
// String response = workflow.route(input, routes); // 假设 route 方法的实现
```
*注：上述代码为示意，`RoutingWorkflow` 的具体实现未在原文中提供。*

### 4.4 编排器

*示意图描述：编排器分析任务，将其分解为子任务，分配给不同的工作单元（Workers）并行处理，最后合并结果。*

#### 使用场景
* 无法预先预测子任务的复杂任务。
* 需要不同方法或观点的任务。
* 需要适应性问题解决的情况。

#### 简单实现代码：
```java
// 假设 OrchestratorResponse, WorkerResponse, OrchestratorWorkersWorkflow 类已定义
// public class OrchestratorWorkersWorkflow {
//     // 假设 ChatClient 通过构造函数或其他方式注入
//     public OrchestratorWorkersWorkflow(ChatClient chatClient) { /* ... */ }

//     public WorkerResponse process(String taskDescription) {
//         // 1. Orchestrator analyzes task and determines subtasks
//         // OrchestratorResponse orchestratorResponse = ... // (LLM call to determine subtasks)

//         // 2. Workers process subtasks in parallel
//         // List<String> workerResponses = ... // (Parallel LLM calls for each subtask)

//         // 3. Results are combined into final response
//         // return new WorkerResponse(/* combined results, analysis */);
//         return null; // Placeholder
//     }
// }
```
#### 使用示例：
```java
// ChatClient chatClient = // ... initialize chat client
// OrchestratorWorkersWorkflow workflow = new OrchestratorWorkersWorkflow(chatClient);

// WorkerResponse response = workflow.process(
//     "Generate both technical and user-friendly documentation for a REST API endpoint"
// );

// System.out.println("Analysis: " + response.analysis());
// System.out.println("Worker Outputs: " + response.workerResponses());
```
*注：上述代码为示意，`OrchestratorWorkersWorkflow` 的具体实现未在原文中提供。*

### 4.5 评估器-优化器

*示意图描述：初始方案生成后，由评估器进行评估，然后优化器根据评估结果对方案进行迭代改进，直至满足标准。*

#### 使用场景
* 存在明确的评估标准。
* 迭代优化提供可衡量的价值。
* 任务受益于多轮批评。

#### 简单代码实现：
```java
// 假设 Generation, EvaluationResponse, RefinedResponse, EvaluatorOptimizerWorkflow 类已定义
// public class EvaluatorOptimizerWorkflow {
//     // 假设 ChatClient 和 context 通过构造函数或其他方式注入
//     public EvaluatorOptimizerWorkflow(ChatClient chatClient /*, SomeContext context*/) { /* ... */ }

//     private Generation generate(String task, Object context) { /* ... */ return null; } // Placeholder
//     private EvaluationResponse evaluate(String response, String task) { /* ... */ return null; } // Placeholder

//     public RefinedResponse loop(String task) {
//         // Generation generation = generate(task, context); // 'context' 未定义
//         // EvaluationResponse evaluation = evaluate(generation.response(), task);
//         // RefinedResponse finalSolution = ... // Logic to refine based on evaluation
//         // String chainOfThought = ... // Record of the process
//         // return new RefinedResponse(finalSolution, chainOfThought);
//         return null; // Placeholder
//     }
// }
```
#### 使用示例：
```java
// ChatClient chatClient = // ... initialize chat client
// EvaluatorOptimizerWorkflow workflow = new EvaluatorOptimizerWorkflow(chatClient);

// RefinedResponse response = workflow.loop(
//     "Create a Java class implementing a thread-safe counter"
// );

// System.out.println("Final Solution: " + response.solution());
// System.out.println("Evolution: " + response.chainOfThought());
```
*注：上述代码为示意，`EvaluatorOptimizerWorkflow` 的具体实现未在原文中提供。*

## 5. 开始使用 Spring AI

开发者可以通过 Maven 中央仓库获取 Spring AI 1.0 的所有组件。使用提供的 bom 导入依赖：

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.ai</groupId>
      <artifactId>spring-ai-bom</artifactId>
      <version>1.0.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

也可以在 **Spring Initializr** 网站上创建 1.0 GA 应用程序，并参考参考文档中的"Getting Started"部分。

---

## DeepSeek API 使用

DeepSeek SDK 的具体使用如下。

**准备工作：**
在 DeepSeek 申请 APIKey。

**添加依赖：**
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-deepseek-spring-boot-starter</artifactId>
</dependency>
```

**设置配置信息 (`application.properties` 或 `application.yml`)：**
```properties
spring.ai.deepseek.api-key=YOUR_API_KEY
spring.ai.deepseek.chat.options.model=deepseek-chat
spring.ai.deepseek.chat.options.temperature=0.8
```

**编写调用代码：**
```java
import org.springframework.ai.chat.model.ChatResponse; // 修正为正确的 ChatResponse 包
import org.springframework.ai.chat.prompt.Prompt; // 引入 Prompt
import org.springframework.ai.chat.prompt.UserMessage; // 引入 UserMessage
import org.springframework.ai.deepseek.DeepSeekChatModel; // 引入 DeepSeekChatModel
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux; // 引入 Flux
import java.util.Map; // 引入 Map

@RestController
public class ChatController {

    private final DeepSeekChatModel chatModel;

    @Autowired
    public ChatController(DeepSeekChatModel chatModel) {
        this.chatModel = chatModel;
    }

    // 普通输出
    @GetMapping("/ai/generate")
    public Map<String, Object> generate(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        // chatModel.call(message) 返回 String，若要符合 Map.of("generation", ...) 结构，需确认返回类型
        // 假设 call 返回的是可以直接放入 Map 的内容，或者其本身就是一个包含 "generation" 键的 Map
        // 为保持一致性，我们假定 call(String) 返回的是一个 ChatResponse 或类似结构，需要从中提取内容
        // 或者，简单地返回 String
        // String responseContent = chatModel.call(message); // 假设返回 String
        // return Map.of("generation", responseContent);

        // 根据 Spring AI 一般实践，call(Prompt) 返回 ChatResponse
        Prompt prompt = new Prompt(new UserMessage(message));
        ChatResponse chatResponse = chatModel.call(prompt);
        return Map.of("generation", chatResponse.getResult().getOutput().getContent());
    }

    // 流式输出
    @GetMapping("/ai/generateStream")
    public Flux<ChatResponse> generateStream(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        var prompt = new Prompt(new UserMessage(message));
        return chatModel.stream(prompt);
    }
}
```

---

## 小结

**Spring AI 1.0** 的发布标志着企业级 Java 应用程序开发进入了一个新时代，使开发者能够轻松地将最先进的 AI 能力集成到他们的 Spring 应用程序中。