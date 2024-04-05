import express from "express";
import "dotenv/config";
import cors from "cors";
import multer from "multer";

const port = process.env.PORT || 3000;

const app = express();
const upload = multer();
app.locals.data = new Map(); // hashmap of qna feedbacks
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

import { reply, feedback, analyze } from "./interview.js";
import { pdfanalyze } from "./pdfanalyzer.js";

app.post("/api/chat", reply);
app.post("/api/feedback", feedback);
app.post("/api/analyze", analyze);

app.post("/api/pdf", upload.single("file"), pdfanalyze);

app.listen(port, () => {
  console.log("Server listening on port", port);
});