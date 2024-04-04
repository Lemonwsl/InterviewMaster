const OPENAI_API_KEY = process.env.OPENAI_API_KEY || null;
import OpenAI from "openai";
import path from "path";

const openai = new OpenAI({
  apiKey: OPENAI_API_KEY,
});

async function reply(req, res) {
  let trigger = [
      {"role": "system", "content": "You are an interviewer and expert in a big company. Your job is only to ask one great question each time"},
      {"role": "user", "content": req.body.message},
  ];

  const params = {
    messages: trigger,
    model: 'gpt-4',
    max_tokens: 100,
  };

  let res_obj = await openai.chat.completions.create(params);
  res.json({ "reply": res_obj.choices[0].message.content});
}

async function feedback(req, res) {
  let system_trigger = "You are an interviewer and expert in a big company. "
                  + "You are a strict and direct person as well. "
                    + "Your job is to analyze and give feedback of given answer from the "
                      + "question: " + req.body.question;
  let user_trigger = "Give me feedback if I answer it like this: " + req.body.answer
  let trigger = [
      {"role": "system", "content": system_trigger},
      {"role": "user", "content": user_trigger},
  ];

  const params = {
    messages: trigger,
    model: 'gpt-4',
    max_tokens: 100,
  };

  let res_obj = await openai.chat.completions.create(params);
  res.json({ "reply": res_obj.choices[0].message.content});
}

export {reply, feedback};