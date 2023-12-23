// Retrieve desired page content (adjust based on your needs)
const contentElements = document.getElementsByClassName("BuildingInfo__BuildingAddress-d8oth5-1");
const content = [];
for (const element of contentElements) {
  content.push(element.textContent);
}

console.log("BELOW IS WHAT I'M LOOKING FOR!!!!!");
console.log(content);

// Send content to background script
chrome.runtime.sendMessage({ action: "sendPageContent", content: content });