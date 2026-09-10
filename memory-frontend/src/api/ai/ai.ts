import request from "@/api/request";
import requestPublicConfig from "@/api/config";

/**
 * AI 智能助手接口层（对接后端真实接口）
 *
 * - createSession / listSessions / listMessages / agentExecute 走 axios 统一封装；
 * - chat 为 SSE 流式（text/event-stream），用 fetch + ReadableStream 消费，
 *   事件类型 text / card / html / done / error。
 */

// 新建会话
export function createSession(data: { agentType: string }): Promise<API.BaseResponse> {
  return request<API.BaseResponse>('/ai/session', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data,
  });
}

// 会话列表
export function listSessions(): Promise<API.BaseResponse> {
  return request<API.BaseResponse>('/ai/session', { method: 'GET' });
}

// 会话历史消息
export function listMessages(sessionId: string): Promise<API.BaseResponse> {
  return request<API.BaseResponse>(`/ai/session/${sessionId}/messages`, { method: 'GET' });
}

// 删除会话（软删会话与消息，后端同步清理运行时记忆）
export function deleteSession(sessionId: string): Promise<API.BaseResponse> {
  return request<API.BaseResponse>(`/ai/session/${sessionId}`, { method: 'DELETE' });
}

// 执行确认的写操作
export function agentExecute(data: { actionId: string }): Promise<API.BaseResponse> {
  return request<API.BaseResponse>('/ai/execute', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data,
  });
}

/** SSE 事件（后端 AiController 推送） */
export interface AiChatEvent {
  /** 事件名：text / card / photos / html / download / table / metric / tool / title / done / error */
  type: string;
  /**
   * 事件载荷：text/html/title 为原文，
   * card/photos/download/table/metric/tool 为 JSON 字符串，error 为错误信息
   */
  data: string;
}

/**
 * SSE 流式聊天（对接后端 POST /ai/chat）。
 *
 * @param data    聊天请求
 * @param onEvent 事件回调，后端每推送一条 SSE 事件触发一次
 */
export async function streamChat(
  data: API.AiChatRequest,
  onEvent: (ev: AiChatEvent) => void,
): Promise<void> {
  const token = localStorage.getItem('token');
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream',
  };
  if (token) headers.Authorization = token;

  const resp = await fetch(`${requestPublicConfig.baseUrl}/ai/chat`, {
    method: 'POST',
    headers,
    body: JSON.stringify(data),
  });

  if (!resp.ok) {
    throw new Error(`AI 服务请求失败（HTTP ${resp.status}）`);
  }
  if (!resp.body) {
    throw new Error('当前浏览器不支持流式响应');
  }

  const reader = resp.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';

  // 按空行分隔 SSE 事件块，逐块解析回调
  const consume = (chunk: string) => {
    buffer += chunk;
    let idx: number;
    while ((idx = buffer.indexOf('\n\n')) >= 0) {
      const block = buffer.slice(0, idx);
      buffer = buffer.slice(idx + 2);
      const ev = parseSseBlock(block);
      if (ev) onEvent(ev);
    }
  };

  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    consume(decoder.decode(value, { stream: true }));
  }
  consume(decoder.decode());
  if (buffer.trim()) {
    const ev = parseSseBlock(buffer);
    if (ev) onEvent(ev);
  }
}

/** 解析单个 SSE 事件块（event: xxx / data: xxx 多行） */
function parseSseBlock(block: string): AiChatEvent | null {
  let type = '';
  const dataLines: string[] = [];
  for (let line of block.split('\n')) {
    if (line.endsWith('\r')) line = line.slice(0, -1);
    if (line.startsWith('event:')) {
      type = line.slice(6).trim();
    } else if (line.startsWith('data:')) {
      let d = line.slice(5);
      if (d.startsWith(' ')) d = d.slice(1); // SSE 规范：data: 后允许单个空格
      dataLines.push(d);
    }
  }
  if (!type && dataLines.length === 0) return null;
  return { type: type || 'message', data: dataLines.join('\n') };
}
