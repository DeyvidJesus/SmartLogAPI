import {createHandler} from '../src/api.mjs';
import {firebaseDependencies} from '../src/firebase.mjs';
import {answerWithGemini} from '../src/gemini.mjs';

let handler;
export default async function smartlog(request) {
  try {
    handler ??= createHandler({...firebaseDependencies(), model: answerWithGemini});
    return await handler(request);
  } catch (error) {
    console.error('SmartLog configuration failure:', error.code || error.name || 'UNKNOWN');
    return Response.json({code: 'CONFIGURATION_FAILURE',
      message: 'A central ainda precisa ser configurada no servidor.'},
    {status: 503, headers: {'Cache-Control': 'no-store'}});
  }
}
export const config = {path: ['/api/v1', '/api/v1/*']};
