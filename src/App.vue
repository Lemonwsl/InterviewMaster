<template>
  <v-app>
    <v-main>
      <v-container fluid>
        <v-row>
          <v-col cols="12">
            <v-card class="pa-3 mx-auto chat-width-modifier">
              <v-card-title class="text-h4">Ai interviewer</v-card-title>
              <v-card-text class="chat-container" ref="chatContainer">
                <div class="messages" ref="messagesContainer">
                  <div
                    v-for="msg in messages"
                    :key="msg.id"
                    :class="{
                      'message-user': msg.sender === 'user',
                      'message-bot': msg.sender === 'bot',
                    }"
                  >
                    <div class="message-content">{{ msg.text }}</div>
                  </div>
                </div>
              </v-card-text>
              <v-card-actions>
                <v-row no-gutters>
                  <v-col cols="7">
                    <v-select
                      v-model="selectedOption"
                      :items="selectionOptions"
                      label="Choose an interviewer"
                      outlined
                      dense
                      class="flex-grow-1"
                    ></v-select>
                  </v-col>
                  <v-col cols="5">
                    <v-file-input
                      v-model="file"
                      label="Upload resume"
                      outlined
                      dense
                      class="flex-grow-1"
                      @change="handleFileUpload"
                      accept=".pdf"
                      prepend-icon=""
                    ></v-file-input>
                  </v-col>
                </v-row>
                <v-row no-gutters>
                  <v-col cols="10">
                    <v-text-field
                      v-model="input"
                      :disabled="isRecording"
                      label="Type a message..."
                      outlined
                      dense
                      class="flex-grow-1"
                      @keyup.enter="sendMessage"
                    ></v-text-field>
                  </v-col>
                  <v-col cols="1" class="d-flex">
                    <v-btn icon @click="toggleRecording">
                      <v-icon v-if="!isRecording">mdi-microphone</v-icon>
                      <v-icon v-else color="red">mdi-stop</v-icon>
                    </v-btn>
                  </v-col>
                  <v-col cols="1" class="d-flex">
                    <v-btn icon color="primary" outlined @click="sendMessage"
                      >Send</v-btn
                    >
                  </v-col>
                </v-row>
              </v-card-actions>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </v-main>
  </v-app>
</template>

<script>
import axios from "axios";

export default {
  data() {
    return {
      // user's input (aka interviewer's input, will be text the whole time)
      input: "",
      // list to store all the inputs to send to backends
      messages: [],
      // store the resume here (pdf)
      file: null,
      isRecording: false,
      recognition: null,
      selectionOptions:['Emily', 'Jack', 'Sophie', 'Oliver', 'Emma', 'Jacob'],
      selectedOption: 'Emily', 
    };
  },
  methods: {
    async sendMessage() {
      // if "type a message" and "add pdf" file are both empty, i will add speaker option
      if (!this.input.trim() && !this.file) return;

      const formData = JSON.parse("{}");

      if (this.file) {
        formData.file = this.file[0];
      }

      if (this.input.trim()) {
        this.messages.push({
          // use Date as unique identifier, so make it id
          id: Date.now(),
          // text user entered
          text: this.input,
          // recognize as user
          sender: "user",
        });
        formData.message = this.input;
        this.input = "";
      }

      formData.interviewer = this.selectedOption;

      try {
        // axios can set the content-type automatically
        const response = await axios.post(
          "http://localhost:3000/api/chat",
          formData,
          {
            headers: { "Content-Type": "multipart/form-data" },
            withCredentials: true,
            credentials: "include",
          }
        );

        if (response.data.audio) {
          const audioBlob = this.base64ToBlob(response.data.audio, "audio/mp3");
          this.playAudio(URL.createObjectURL(audioBlob));
        }
        this.messages.push({
          id: Date.now(),
          text: response.data.reply,
          sender: "bot",
        });

      } catch (error) {
        console.error("Failed to send message: ", error);
      }
    },

    // convert base64 to audio
    base64ToBlob(base64, contentType) {
      const byteCharacters = atob(base64);
      const byteNumbers = new Array(byteCharacters.length);
      for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
      }
      const byteArray = new Uint8Array(byteNumbers);
      return new Blob([byteArray], { type: contentType });
    },

    // play audio
    playAudio(audioUrl) {
      const audio = new Audio(audioUrl);
      audio
        .play()
        .catch((error) => console.error("Error playing audio:", error));
    },
    handleFileUpload() {},

    toggleRecording() {
      if (!this.isRecording) {
        this.startRecording();
      } else {
        this.stopRecording();
      }
    },

    startRecording() {
      if (!("webkitSpeechRecognition" in window)) {
        alert(
          "Your browser does not support speech recognition. Please try Chrome."
        );
        return;
      }

      const recognition = new webkitSpeechRecognition();
      recognition.lang = "en-US";
      recognition.continuous = false;
      recognition.interimResults = false;

      recognition.onstart = () => {
        this.isRecording = true;
      };

      recognition.onresult = (event) => {
        const transcript = event.results[0][0].transcript;
        this.input = transcript;
        this.sendMessage();
      };

      recognition.onerror = (event) => {
        console.error("Speech recognition error", event.error);
      };

      recognition.onend = () => {
        this.isRecording = false;
      };

      this.recognition = recognition;
      recognition.start();
    },

    stopRecording() {
      if (this.recognition) {
        this.recognition.stop();
      }
    },
  },
};
</script>

<style scoped>
.chat-container {
  height: 77.5vh;
  overflow-y: auto;
}
.messages {
  display: flex;
  flex-direction: column;
}
.message-user {
  max-width: 85%;
  font-size: 0.9rem;
  border-radius: 6px;
  align-self: flex-end;
  background-color: #4a76a8;
  color: #ffffff;
  margin-bottom: 4px;
}
.message-bot {
  max-width: 85%;
  font-size: 0.9rem;
  border-radius: 6px;
  align-self: flex-start;
  background-color: #8d8d8d;
  color: #ffffff;
  margin-bottom: 4px;
}
.message-content {
  margin: 5px;
  padding: 10px;
  border-radius: 12;
}
.v-container {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  max-width: 960px;
  margin-left: auto;
  margin-right: auto;
}
.chat-width-modifier {
  border-radius: 15px;
  box-shadow: 0 0px 48px rgba(0, 0, 0, 0.2);
  max-width: 960px;
}

/* bigger screen use bigger font */
@media (min-width: 600px) {
  .message-user,
  .message-bot {
    font-size: 1.2rem;
  }
}
</style>
