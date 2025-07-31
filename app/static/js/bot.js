document.addEventListener("DOMContentLoaded", function () {
  // Language dropdown change
  document
    .getElementById("language-select")
    ?.addEventListener("change", function () {
      $.ajax({
        url: "/",
        type: "POST",
        data: {
          language: this.value,
        },
        success: function (response) {
          // Optional display:
          // document.getElementById('selected-language-display').innerText = 'You selected: ' + response.selected_language;
        },
        error: function () {
          alert("Error selecting language!");
        },
      });
    });

  // QA form submit
  document
    .getElementById("qa-form")
    ?.addEventListener("submit", function (event) {
      event.preventDefault();
      const inputVal = document.querySelector("input[name='user_input']").value;

      fetch("/", {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
        },
        body: new URLSearchParams({
          user_input: inputVal,
        }),
      })
        .then((response) => response.json())
        .then((data) => {
          const user_message =
            "<li><strong>You:</strong> " + data.user_input + "</li>";
          const bot_message =
            "<li><strong>Bot:</strong> " + data.answer + "</li><br>";

          document
            .querySelector("ul")
            .insertAdjacentHTML("beforeend", user_message + bot_message);
          document.querySelector("input[name='user_input']").value = "";

          // Auto-scroll to bottom
          document.getElementById("chat-container").scrollTop =
            document.getElementById("chat-container").scrollHeight;
        })
        .catch(() => {
          alert("Error submitting input!");
        });
    });
});
