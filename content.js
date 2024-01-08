// Retrieve desired page content (adjust based on your needs)
const contentElements = document.querySelectorAll("div.summary-container h1");

console.log("Address scraped: " + contentElements);

const content = [];
for (const element of contentElements) {
  content.push(element.textContent);
}

console.log("Server: " + content);

alert("GOOT HERE");


// const displayAddress = document.getElementById('address');
// displayAddress.innerHTML = contentElements;

// // Clear the list
// while (list.firstChild) {
//   list.removeChild(list.firstChild);
// }

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

let counter = 0;

// Request supermarket data from server
fetch('http://localhost:8080/get-supermarkets')
  .then(response => response.json())
  .then(data => {
    console.log("Successfully received /get-supermarket response!");
    console.log("Data from /get-supermarket: " + data);

    // Select the element in your HTML file where you want to display the data
    console.log("Before displayElement");
    const displayElement = document.getElementById('display');

    while(displayElement.firstChild) {
      displayElement.removeChild(displayElement.firstChild);
    }

    counter++;
    const counterElement = document.getElementById('counter');
    counterElement.innerHTML = counter;
    console.log("Counter: " + counter);
    
    console.log("After displayElement");
    displayElement.innerHTML = ''; // Clear any existing content
    console.log("After displayElement.innerHTML");

    // Create an ordered list to display the supermarkets
    console.log("Before ol");
    const list = document.createElement('ol');
    console.log("After ol");
    displayElement.appendChild(list);
    console.log("After displayElement.appendChild(list)");

    // Create a new list item for each supermarket
    data.elements.slice(0, 3).forEach(supermarket => {
      const name = supermarket.tags.name;
      console.log("Supermarket name: " + name);
      const lat = supermarket.lat;
      console.log("Supermarket lat: " + lat);
      const lon = supermarket.lon;
      console.log("Supermarket lon: " + lon);
      const url = `https://www.google.com/maps/?q=${lat},${lon}`;
      console.log("Supermarket Google Maps url: " + url);

      const listItem = document.createElement('li');
      const link = document.createElement('a');
      link.href = url;
      link.textContent = name;
      link.target = "_blank"; // Opens in a new tab

      listItem.appendChild(link);
      list.appendChild(listItem);
    });
})
.catch((error) => {
  console.error('Error:', error);
});
