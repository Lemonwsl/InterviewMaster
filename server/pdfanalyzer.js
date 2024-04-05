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

async function savePdf(req, res) {
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

  const vectorIndex = new MemoryVectorIndex;

  await upsertIntoVectorIndex({
    vectorIndex,
    embeddingModel,
    objects: chunks,
    getValueToEmbed: (chunk) => chunk.text,
  });

  req.app.locals.vectorIndex = vectorIndex;
  req.app.locals.embeddingModel = embeddingModel;

  res.json({ "status": "Success"});
}

async function questionPdf(req, res) {
  const question = req.body.topic;

  // hypothetical document embeddings:
  const hypotheticalAnswer = await generateText({
    // use cheaper model to generate hypothetical answer:
    model: openai.ChatTextGenerator({
      model: "gpt-3.5-turbo",
      temperature: 0.4,
    }),
    prompt: [
      openai.ChatMessage.system(`Act as an interviewer. Question the user's topic!`),
      openai.ChatMessage.user(question),
    ],
  });

  vectorIndex = req.app.locals.vectorIndex;
  embeddingModel = req.app.locals.embeddingModel;
  
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
  const answer = await generateText({
    // use stronger model to answer the question:
    model: openai.ChatTextGenerator({ model: "gpt-4", temperature: 0.4 }),
    prompt: [
      openai.ChatMessage.system(
        // Instruct the model on how to answer:
        `Question the user's topic using only the provided information.\n` +
          // Provide some context:
          `Include the page number of the information that you are using.\n` +
          // To reduce hallucination, it is important to give the model an answer
          // that it can use when the information is not sufficient:
          `If the user's topic cannot be questioned using the provided information, ` +
          `respond with a random interview question.`
      ),
      openai.ChatMessage.user(question),
      openai.ChatMessage.fn({
        fnName: "getInformation",
        content: JSON.stringify(information),
      }),
    ],
  });
}

async function pdfanalyze(req, res) {
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

  const vectorIndex = new MemoryVectorIndex;

  await upsertIntoVectorIndex({
    vectorIndex,
    embeddingModel,
    objects: chunks,
    getValueToEmbed: (chunk) => chunk.text,
  });

  const question = req.body.topic;

  // hypothetical document embeddings:
  const hypotheticalAnswer = await generateText({
    // use cheaper model to generate hypothetical answer:
    model: openai.ChatTextGenerator({
      model: "gpt-3.5-turbo",
      temperature: 0.4,
    }),
    prompt: [
      openai.ChatMessage.system(`Act as an interviewer. Question the user's topic!`),
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
  const answer = await generateText({
    // use stronger model to answer the question:
    model: openai.ChatTextGenerator({ model: "gpt-4", temperature: 0.4 }),
    prompt: [
      openai.ChatMessage.system(
        // Instruct the model on how to answer:
        `Question the user's topic using only the provided information.\n` +
          // Provide some context:
          `Include the page number of the information that you are using.\n` +
          // To reduce hallucination, it is important to give the model an answer
          // that it can use when the information is not sufficient:
          `If the user's topic cannot be questioned using the provided information, ` +
          `respond with a random interview question.`
      ),
      openai.ChatMessage.user(question),
      openai.ChatMessage.fn({
        fnName: "getInformation",
        content: JSON.stringify(information),
      }),
    ],
  });

  res.json({ "reply": answer});
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

export { pdfanalyze };