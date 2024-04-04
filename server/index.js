import express from "express";
import "dotenv/config"

const port = process.env.PORT || 3000;

const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

import reply from "./interview.js";

app.get("/api/v1/hello", (req, res) => {
  res.json({ "reply": reply(req.data.message)});
});

app.listen(port, () => {
  console.log("Server listening on port", port);
});