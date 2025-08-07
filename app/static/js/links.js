function toggleInput() {
  const selected = document.querySelector(
    'input[name="link_type"]:checked'
  ).value;
  const singleInput = document.getElementById("single_input");
  const batchWrapper = document.getElementById("batch_input_wrapper");

  if (selected === "single") {
    singleInput.style.display = "block";
    singleInput.required = true;
    batchWrapper.style.display = "none";
    document.getElementById("batch_links").required = false;
  } else if (selected === "batch") {
    singleInput.style.display = "none";
    singleInput.required = false;
    batchWrapper.style.display = "block";
    document.getElementById("batch_links").required = true;
  } else if (selected === "playlist") {
    singleInput.style.display = "block";
    singleInput.required = true;
    batchWrapper.style.display = "none";
    document.getElementById("batch_links").required = false;
  }
}

// Initialize on load in case form is pre-filled
//window.onload = toggleInput;
