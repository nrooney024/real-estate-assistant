// Retrieve desired page content (adjust based on your needs)
const contentElements = document.getElementsByClassName("BuildingInfo__BuildingAddress-d8oth5-1");
const content = [];
for (const element of contentElements) {
  content.push(element.textContent);
}

console.log("Server: " + content);


// Send adddress to server
fetch('http://localhost:8080/receive-data', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({ content: content })
})
.then(response => response.text())
.then(data => console.log(data))
.catch((error) => console.error('Error:', error));



// Request supermarket data from server
fetch('http://localhost:8080/get-supermarkets')
  .then(response => response.json())
  .then(data => {
    console.log("Successfully received /get-supermarket response!");
    console.log(data);
  })
  .catch((error) => {
    console.error('Error:', error);
});