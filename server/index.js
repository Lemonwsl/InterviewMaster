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

import { chat, feedback, detailedFeedback } from "./script.js";
import { chatMobile, feedbackMobile, detailedFeedbackMobile } from "./mobileScript.js";

app.post("/api/chat", upload.single("file"), chat);
app.post("/api/feedback", feedback);
app.post("/api/detailedfeedback", detailedFeedback);

app.post("/api/mobile/chat", upload.single("file"), chat);
app.post("/api/mobile/feedback", feedback);
app.post("/api/mobile/detailedfeedback", detailedFeedback);

app.listen(port, () => {
  console.log("Server listening on port", port);
});