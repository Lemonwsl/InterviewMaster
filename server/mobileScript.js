const OPENAI_API_KEY = process.env.OPENAI_API_KEY || null;
import OpenAI from "openai";
import path from "path";
import { Command } from "commander";
import fs from "fs/promises";
import {
  MemoryVectorIndex,
  VectorIndexRetriever,
  generateText,
  openai,
  retrieve,
  splitAtToken,
  splitTextChunks,
  streamText,
  upsertIntoVectorIndex,
} from "modelfusion";
import * as readline from "node:readline/promises";
import * as PdfJs from "pdfjs-dist/legacy/build/pdf.mjs";

async function chatMobile(req, res) {
  let ran = Math.random() * 2; // randomize so person with file is not always asked about the file
  if (req.file != undefined && ran < 1) {
    const pages = await loadPdfPages(req.file.buffer);

    const embeddingModel = openai.TextEmbedder({
      model: "text-embedding-ada-002",
    });

    const chunks = await splitTextChunks(
      splitAtToken({
        maxTokensPerChunk: 256,
        tokenizer: embeddingModel.tokenizer,
      }),
      pages
    );

    const vectorIndex = new MemoryVectorIndex();

    await upsertIntoVectorIndex({
      vectorIndex,
      embeddingModel,
      objects: chunks,
      getValueToEmbed: (chunk) => chunk.text,
    });

    let ran_topic = Math.random() * 4; // random out topic
    let question;
    if (ran_topic < 1) {
      question = "work experience";
    } else if (ran_topic < 2) {
      question = "skills";
    } else if (ran_topic < 3) {
      question = "projects";
    } else {
      question = "education";
    }

    let answer;
    try {
      // hypothetical document embeddings:
      const hypotheticalAnswer = await generateText({
        // use cheaper model to generate hypothetical answer:
        model: openai.ChatTextGenerator({
          model: "gpt-3.5-turbo",
          temperature: 0.4,
        }),
        prompt: [
          openai.ChatMessage.system(
            `Act as an interviewer. Question the user's topic!`
          ),
          openai.ChatMessage.user(question),
        ],
      });

      // search for text chunks that are similar to the hypothetical answer:
      const information = await retrieve(
        new VectorIndexRetriever({
          vectorIndex,
          embeddingModel,
          maxResults: 5,
          similarityThreshold: 0.75,
        }),
        hypotheticalAnswer
      );

      // answer the user's question using the retrieved information:
      answer = await generateText({
        // use stronger model to answer the question:
        model: openai.ChatTextGenerator({
          model: "gpt-3.5-turbo",
          temperature: 0.4,
        }),
        prompt: [
          openai.ChatMessage.system(
            // Instruct the model on how to answer:
            `Question the user's topic using only the provided information.\n` +
              `However, before that you may give a very little feedback on the answer.\n` +
              // Provide some context:
              `Include the page number of the information that you are using.\n` +
              `When including the number of page, also said that it's from resume.\n` +
              // To reduce hallucination, it is important to give the model an answer
              // that it can use when the information is not sufficient:
              `If the user's topic cannot be questioned using the provided information, ` +
              `respond with a random interview question, without saying other thing.`
          ),
          openai.ChatMessage.user(question),
          openai.ChatMessage.fn({
            fnName: "getInformation",
            content: JSON.stringify(information),
          }),
        ],
      });
    } catch (e) {
      console.log(e);
      answer = "sorry, but can you repeat it with different wordings?";
    }

    let arr_qna = await req.app.locals.data.get(req.body.name);
    if (!arr_qna) {
      req.app.locals.data.set(req.body.name, [])
    }
    arr_qna = await req.app.locals.data.get(req.body.name);
    arr_qna.push({ role: "user", content: req.body.message });
    arr_qna.push({ role: "assistant", content: answer });
    res.json({ reply: answer });
  } else {
    const openai = new OpenAI({
      apiKey: OPENAI_API_KEY,
    });

    let trigger;
    if (!req.app.locals.data.get(req.body.name)) {
      trigger = [
        {
          role: "system",
          content:
            "You are an interviewer and expert in a big company. Your job is only to ask one great question each time",
        },
        { role: "user", content: req.body.message },
      ];
    } else {
      let qna_list = req.app.locals.data.get(req.body.name);
      trigger = [
        {
          role: "system",
          content:
            "You are an interviewer and expert in a big company. Your job is only to ask one great question each time, but before that you may give a very little feedback on the answer",
        },
      ];
      trigger.concat(qna_list);
      trigger.push({ role: "user", content: req.body.message });
    }

    let answer;
    try {
      const params = {
        messages: trigger,
        model: "gpt-3.5-turbo",
        max_tokens: 100,
      };
      let res_obj = await openai.chat.completions.create(params);
      answer = res_obj.choices[0].message.content;
    } catch (e) {
      console.log(e);
      answer = "sorry, but can you repeat it with different wordings?";
    }
    let arr_qna = await req.app.locals.data.get(req.body.name);
    if (!arr_qna) {
       req.app.locals.data.set(req.body.name, []);
    }
    arr_qna = await req.app.locals.data.get(req.body.name);
    arr_qna.push({ role: "user", content: req.body.message });
    arr_qna.push({ role: "assistant", content: answer });
    res.json({ reply: answer });
  }
}

async function loadPdfPages(pdfData) {
  // parse the PDF file:
  const pdf = await PdfJs.getDocument({
    data: new Uint8Array(
      pdfData.buffer,
      pdfData.byteOffset,
      pdfData.byteLength
    ),
    useSystemFonts: true, // see https://github.com/mozilla/pdf.js/issues/4244#issuecomment-1479534301
  }).promise;

  const pageTexts = [];

  // extract text from each page:
  for (let i = 0; i < pdf.numPages; i++) {
    const page = await pdf.getPage(i + 1);
    const pageContent = await page.getTextContent();
    const text = pageContent.items
      // limit to TextItem, extract str:
      .filter((item) => item.str != null)
      .map((item) => item.str)
      .join(" ")
      .replace(/\s+/g, " ") // reduce multiple whitespaces to single space
      .trim();

    if (text.length > 0)
      pageTexts.push({
        pageNumber: i + 1,
        text,
      });
  }

  return pageTexts;
}

async function feedbackMobile(req, res) {
  let feedbacks = "";
  let qna_list = req.app.locals.data.get(req.body.name);
  if (qna_list) {
    qna_list.pop();
  }
  while (qna_list && qna_list.length > 1) {
    const answer = qna_list.pop().content;
    const question = qna_list.pop().content;

    let system_trigger =
      "You are an interviewer and expert in a big company. " +
      "You are a strict and direct person as well. " +
      "Your job is to analyze and give feedback of given answer from the " +
      "question: " +
      question;
    let user_trigger = "Give me feedback if I answer it like this: " + answer;
    let trigger = [
      { role: "system", content: system_trigger },
      { role: "user", content: user_trigger },
    ];

    const params = {
      messages: trigger,
      model: "gpt-3.5-turbo",
      max_tokens: 100,
    };
    const openai = new OpenAI({
      apiKey: OPENAI_API_KEY,
    });

    let single_feedback = "";
    let res_obj = await openai.chat.completions.create(params);
    single_feedback += 'Question: "' + question + '"\n';
    single_feedback += 'Answer: "' + answer + '"\n';
    single_feedback +=
      "Feedback: " + res_obj.choices[0].message.content + "\n\n";

    feedbacks = single_feedback + feedbacks;

    let sum_trigger = [
      { role: "system", content: "You are a summarizer. Your job is to text" },
      { role: "user", content: "summarize this: " + feedbacks },
    ];
    res_obj = await openai.chat.completions.create(params);
    feedbacks = res_obj.choices[0].message.content;
  }
  res.json({ message: feedbacks });
}

async function detailedFeedbackMobile(req, res) {
  let feedbacks = "";
  let qna_list = req.app.locals.data.get(req.body.name);
  if (qna_list) {
    qna_list.pop();
  }
  while (qna_list && qna_list.length > 1) {
    const answer = qna_list.pop().content;
    const question = qna_list.pop().content;

    let system_trigger =
      "You are an interviewer and expert in a big company. " +
      "You are a strict and direct person as well. " +
      "Your job is to analyze and give feedback of given answer from the " +
      "question: " +
      question;
    let user_trigger = "Give me feedback if I answer it like this: " + answer;
    let trigger = [
      { role: "system", content: system_trigger },
      { role: "user", content: user_trigger },
    ];

    const params = {
      messages: trigger,
      model: "gpt-3.5-turbo",
      max_tokens: 100,
    };
    const openai = new OpenAI({
      apiKey: OPENAI_API_KEY,
    });

    let single_feedback = "";
    let res_obj = await openai.chat.completions.create(params);
    single_feedback += 'Question: "' + question + '"\n';
    single_feedback += 'Answer: "' + answer + '"\n';
    single_feedback +=
      "Feedback: " + res_obj.choices[0].message.content + "\n\n";

    feedbacks = single_feedback + feedbacks;
  }
  res.json({ message: feedbacks });
}

export { chatMobile, feedbackMobile, detailedFeedbackMobile };
