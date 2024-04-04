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
  if (!req.app.locals.data.has("user_in_session")) {
    req.app.locals.data.set("user_in_session", []);
  } else {
    let questions = req.app.locals.data.get("user_in_session");
    let question = questions.pop();
    let answer = req.body.message;
    let qna_json = {"question": question, "answer": answer};
    req.app.locals.data.get("user_in_session").push(qna_json);
  }
  req.app.locals.data.get("user_in_session").push(res_obj.choices[0].message.content);
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

async function feedbackAll(qna_list) {
  let feedbacks = [];
  for (const qna_json of qna_list) {
    let question = qna_json.question;
    let answer = qna_json.answer;

    let system_trigger = "You are an interviewer and expert in a big company. "
                    + "You are a strict and direct person as well. "
                      + "Your job is to analyze and give feedback of given answer from the "
                        + "question: " + question;
    let user_trigger = "Give me feedback if I answer it like this: " + answer
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
    qna_json.feedback = res_obj.choices[0].message.content
    feedbacks.push(qna_json);
  }
  
  return feedbacks;
}

async function analyze(req, res) {
  let qna_list = req.app.locals.data.get("user_in_session");
  if (qna_list != undefined) {
    qna_list.pop(); // delete unanswered
    let feedbacks = await feedbackAll(qna_list);
    res.json(feedbacks);
  } else res.json({});
}

export {reply, feedback, analyze};