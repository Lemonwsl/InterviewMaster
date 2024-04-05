import express from "express";
import "dotenv/config";
import cors from "cors";
import multer from "multer";
import cookieParser from "cookie-parser";

const port = process.env.PORT || 3000;

const app = express();
const upload = multer();
app.locals.data = new Map(); // hashmap of qna feedbacks
app.locals.numUser = 1;
app.use(cors({ origin: true, credentials: true }));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(cookieParser());

import { chat, dataCheck } from "./script.js";

app.post("/api/chat", upload.single("file"), chat);
app.post("/api/check", dataCheck);

app.listen(port, () => {
  console.log("Server listening on port", port);
});