import express from "express";
import "dotenv/config";
import cors from "cors";

const port = process.env.PORT || 3000;

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

import reply from "./interview.js";

app.post("/api/chat", reply);

app.listen(port, () => {
  console.log("Server listening on port", port);
});