import {ApiError} from './schema.mjs';

export async function answerWithGemini({instructions, context, question},
    env = process.env, fetchImpl = fetch) {
  if (!env.GEMINI_API_KEY) throw new ApiError(503, 'Configure GEMINI_API_KEY no servidor para ativar o assistente.');
  let response;
  for (let attempt = 0; attempt < 2; attempt++) {
  try {
    response = await fetchImpl('https://generativelanguage.googleapis.com/v1beta/openai/chat/completions', {
      method: 'POST', headers: {'Content-Type': 'application/json', Authorization: 'Bearer ' + env.GEMINI_API_KEY},
      body: JSON.stringify({model: env.GEMINI_MODEL || 'gemini-3.1-flash-lite', max_tokens: 2048, reasoning_effort: 'minimal',
        messages: [{role: 'system', content: instructions},
          {role: 'user', content: 'DADOS DA OPERAÇÃO:\n' + JSON.stringify(context) + '\nPERGUNTA:\n' + question}]}),
      signal: AbortSignal.timeout(12000),
    });
  } catch {
    if (attempt === 0) continue;
    throw new ApiError(502, 'Não foi possível consultar o assistente.');
  }
  if ([500, 502, 503, 504].includes(response.status) && attempt === 0) {
    await new Promise(resolve => setTimeout(resolve, 500));
    continue;
  }
  break;
  }
  if (response.status === 429) throw new ApiError(429, 'Quota do assistente atingida. Tente mais tarde.');
  if ([401, 402, 403].includes(response.status)) throw new ApiError(503, 'Credencial ou plano gratuito do assistente indisponível.');
  if (!response.ok) throw new ApiError(502, 'Provedor de IA indisponível.');
  let body;
  try { body = await response.json(); } catch { throw new ApiError(502, 'O assistente devolveu um formato inesperado.'); }
  const text = body?.choices?.[0]?.message?.content;
  if (typeof text !== 'string' || !text.trim()) throw new ApiError(502, 'O assistente não retornou conteúdo.');
  return text.trim();
}
