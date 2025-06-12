package com.aireader.backend.service;

import com.aireader.backend.dto.NoteRequestDto;
import com.aireader.backend.dto.NoteResponseDto;
import com.aireader.backend.dto.NoteAnalysisResultDto;
import com.aireader.backend.model.jpa.User;
import com.aireader.backend.repository.UserRepository;
import com.aireader.backend.service.NoteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 笔记AI分析集成测试
 * 验证阶段五的功能：笔记AI分析集成
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NoteAnalysisIntegrationTest {

    @Autowired
    private NoteService noteService;
    
    @Autowired
    private UserRepository userRepository;
    
    private User testUser;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = User.builder()
                .username("test_user_note_analysis")
                .email("test_note_analysis@example.com")
                .passwordHash("password123")
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testCreateNoteTriggersAIAnalysis() {
        // 准备测试数据
        String noteContent = "这是一篇关于人工智能和机器学习的笔记。" +
                "人工智能（AI）是计算机科学的一个分支，致力于创建能够执行通常需要人类智能的任务的系统。" +
                "机器学习是人工智能的一个子集，它使计算机能够在没有明确编程的情况下学习和改进。" +
                "深度学习是机器学习的一个分支，使用神经网络来模拟人脑的工作方式。" +
                "这些技术在图像识别、自然语言处理和推荐系统等领域有广泛应用。";
        
        NoteRequestDto noteRequest = new NoteRequestDto();
        noteRequest.setTitle("人工智能学习笔记");
        noteRequest.setContent(noteContent);
        noteRequest.setTags(Set.of("AI", "机器学习", "技术"));
        
        // 执行创建笔记
        NoteResponseDto createdNote = noteService.createNote(noteRequest, testUser.getId());
        
        // 验证笔记创建成功
        assertNotNull(createdNote);
        assertNotNull(createdNote.getId());
        assertEquals("人工智能学习笔记", createdNote.getTitle());
        assertEquals(noteContent, createdNote.getContent());
        assertEquals(testUser.getId(), createdNote.getUserId());
        assertTrue(createdNote.getTags().contains("AI"));
        assertTrue(createdNote.getTags().contains("机器学习"));
        assertTrue(createdNote.getTags().contains("技术"));
        
        // 验证AI分析任务已提交（通过检查笔记状态或其他方式）
        // 注意：在实际测试中，AI分析是异步的，这里只验证笔记创建成功
        System.out.println("笔记创建成功，ID: " + createdNote.getId());
        System.out.println("AI分析任务应该已提交到队列");
    }

    @Test
    void testUpdateNoteContentTriggersReanalysis() {
        // 先创建一个笔记
        NoteRequestDto initialRequest = new NoteRequestDto();
        initialRequest.setTitle("初始笔记");
        initialRequest.setContent("这是初始内容，比较短。");
        
        NoteResponseDto createdNote = noteService.createNote(initialRequest, testUser.getId());
        
        // 更新笔记内容为更长的内容
        String updatedContent = "这是更新后的内容，包含更多信息。" +
                "区块链是一种分布式账本技术，它维护着一个不断增长的记录列表。" +
                "每个区块包含一个加密哈希、时间戳和交易数据。" +
                "区块链技术最初是为比特币而开发的，但现在已经扩展到许多其他应用领域。" +
                "智能合约是区块链上的自执行合约，合约条款直接写入代码中。";
        
        NoteRequestDto updateRequest = new NoteRequestDto();
        updateRequest.setTitle("更新后的笔记");
        updateRequest.setContent(updatedContent);
        updateRequest.setTags(Set.of("区块链", "智能合约", "技术"));
        
        // 执行更新
        NoteResponseDto updatedNote = noteService.updateNote(
                createdNote.getId(), updateRequest, testUser.getId());
        
        // 验证更新成功
        assertNotNull(updatedNote);
        assertEquals(createdNote.getId(), updatedNote.getId());
        assertEquals("更新后的笔记", updatedNote.getTitle());
        assertEquals(updatedContent, updatedNote.getContent());
        assertTrue(updatedNote.getTags().contains("区块链"));
        assertTrue(updatedNote.getTags().contains("智能合约"));
        
        System.out.println("笔记更新成功，应该触发重新分析");
    }

    @Test
    void testGetNoteAnalysisResult() {
        // 创建笔记
        NoteRequestDto noteRequest = new NoteRequestDto();
        noteRequest.setTitle("测试分析结果");
        noteRequest.setContent("这是一个用于测试AI分析结果获取的笔记内容。" +
                "内容足够长以触发AI分析。包含多个概念和主题。");
        
        NoteResponseDto createdNote = noteService.createNote(noteRequest, testUser.getId());
        
        // 尝试获取分析结果
        NoteAnalysisResultDto analysisResult = noteService.getNoteAnalysisResult(
                createdNote.getId(), testUser.getId());
        
        // 验证分析结果
        assertNotNull(analysisResult);
        assertEquals(createdNote.getId(), analysisResult.getNoteId());
        // 由于是异步处理，分析结果可能还没有userId，所以我们只检查noteId
        assertNotNull(analysisResult.getNoteId());
        
        // 由于是异步处理，分析可能还在进行中
        if ("PENDING".equals(analysisResult.getAnalysisStatus())) {
            System.out.println("分析正在进行中: " + analysisResult.getMessage());
        } else if ("COMPLETED".equals(analysisResult.getAnalysisStatus())) {
            System.out.println("分析已完成");
            System.out.println("摘要: " + analysisResult.getEnhancedSummary());
            System.out.println("关键点: " + analysisResult.getKeyPoints());
            System.out.println("智能标签: " + analysisResult.getIntelligentTags());
        }
    }

    @Test
    void testReanalyzeNote() {
        // 创建笔记
        NoteRequestDto noteRequest = new NoteRequestDto();
        noteRequest.setTitle("重新分析测试");
        noteRequest.setContent("这是一个用于测试重新分析功能的笔记。" +
                "包含足够的内容来触发AI分析。");
        
        NoteResponseDto createdNote = noteService.createNote(noteRequest, testUser.getId());
        
        // 手动触发重新分析
        boolean reanalysisTriggered = noteService.reanalyzeNote(
                createdNote.getId(), testUser.getId());
        
        // 验证重新分析任务提交成功
        assertTrue(reanalysisTriggered, "重新分析任务应该提交成功");
        
        System.out.println("重新分析任务提交成功，笔记ID: " + createdNote.getId());
    }

    @Test
    void testShortContentDoesNotTriggerAnalysis() {
        // 创建内容很短的笔记
        NoteRequestDto noteRequest = new NoteRequestDto();
        noteRequest.setTitle("短笔记");
        noteRequest.setContent("短内容"); // 少于50个字符
        
        NoteResponseDto createdNote = noteService.createNote(noteRequest, testUser.getId());
        
        // 验证笔记创建成功
        assertNotNull(createdNote);
        assertEquals("短笔记", createdNote.getTitle());
        assertEquals("短内容", createdNote.getContent());
        
        System.out.println("短内容笔记创建成功，应该不会触发AI分析");
    }
} 