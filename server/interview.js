const OPENAI_API_KEY = process.env.OPENAI_API_KEY || null;
import OpenAI from "openai";
import path from "path";

const openai = new OpenAI({
  apiKey: OPENAI_API_KEY,
});

export default async function reply(message) {
  let trigger = [
      {"role": "system", "content": "You are an interviewer and expert in a big company"},
      {"role": "user", "content": message},
  ];

  const params = {
    messages: trigger,
    model: 'gpt-4',
    max_tokens: 100,
  };

  res_obj = await openai.chat.completions.create(params);
  return res_obj.choices[0].message.content;

}