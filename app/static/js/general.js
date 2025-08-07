document.addEventListener("DOMContentLoaded", function () {
  const loader = document.getElementById("loader");

  if (!loader) return;

  const form = document.querySelector("form");

  if (form) {
    form.addEventListener("submit", function () {
      setTimeout(() => {
        loader.style.display = "block";
      }, 200);
    });
  }
});
