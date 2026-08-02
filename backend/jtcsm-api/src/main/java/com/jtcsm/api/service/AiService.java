package com.jtcsm.api.service;

import com.jtcsm.common.dto.AiFeedbackRequest;
import com.jtcsm.common.dto.AiGenerateRecordVO;
import com.jtcsm.common.dto.AiGenerateRequest;
import com.jtcsm.common.dto.AiGenerateResponse;
import java.util.List;

/**
 * AI 服务接口 —— AI 菜谱生成、历史、收藏和反馈
 */
public interface AiService {
    AiGenerateResponse generate(Long userId, AiGenerateRequest request);
    List<AiGenerateRecordVO> getHistory(Long userId, int page, int size);
    AiGenerateRecordVO getHistoryDetail(Long userId, Long id);
    void saveToFavorite(Long userId, Long historyId);
    void submitFeedback(Long userId, AiFeedbackRequest request);
}
