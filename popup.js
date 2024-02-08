// document.addEventListener('DOMContentLoaded', function() {
//     // When the popup DOM is fully loaded, retrieve the supermarket data.
//     chrome.storage.local.get(['supermarkets'], function(result) {
//       if(result.supermarkets) {
//         // If supermarkets are found in storage, display them.
//         console.log("result.supermarkets from popup.js chrome storage: ", result.supermarkets)
//         displaySupermarkets(result.supermarkets);
//       }
//     });
//   });

  document.getElementById('searchForm').addEventListener('submit', function(event) {
    console.log("Form submitted...")
    event.preventDefault(); // Prevent the form from submitting normally
    const searchQuery = document.getElementById('searchQuery').value;
  
    function fetchSupermarkets(searchQuery) {
      // Use `fetch` to send the address to your API and get the list of supermarkets
      // Then, store it using chrome.storage or send it directly to your popup
      
      console.log("fetchSupermarkets called...")
      
      fetch('http://localhost:8080/fetch-address-data', {
              method: 'POST',
              headers: {
                  'Content-Type': 'application/json'
              },
              body: JSON.stringify({ content: searchQuery })
          })
          .then(response => response.json())
          .then(data => {
              console.log(data)
              console.log("Successfully received /fetch-address-data response!");
              console.log("Data from /fetch-address-data: " + data);
              
          chrome.storage.local.set({ supermarkets: data }, function() {
              console.log('Supermarkets are saved in chrome.storage.local');
          });
          
          displaySupermarkets(data)
      })
      .catch(error => {
          console.error('Error fetching supermarket data:', error);
      });
    }
  });
  

  
function displaySupermarkets(supermarkets) {
    const displayElement = document.getElementById('display');

    // Clear any existing content
    while (displayElement.firstChild) {
        displayElement.removeChild(displayElement.firstChild);
    }

    // Create an ordered list to display the supermarkets
    const list = document.createElement('ol');
    displayElement.appendChild(list);


    // Updating address
    const address = `Apartment Address: ${supermarkets["received-address"]["content"]}`
    console.log("Address from displaySupermarkets in popup.js: " + address)
    document.getElementById('address').textContent = address

    // Access the array of supermarket elements
    const supermarketElements = supermarkets["closest-supermarkets"];

    // Slice the first three supermarkets if you only want to display a subset
    const firstThreeSupermarkets = supermarketElements.slice(0, 5);

    // Iterate over the supermarkets
    firstThreeSupermarkets.forEach(supermarket => {
        // Extract the necessary information
        const name = supermarket.name;
        const lat = supermarket.lat;
        const lon = supermarket.lon;
        const url = `https://www.google.com/maps/?q=${lat},${lon}`;

        // Create and append the list item
        const listItem = document.createElement('li');
        const link = document.createElement('a');
        link.href = url;
        link.textContent = name;
        link.target = "_blank"; // Opens in a new tab

        listItem.appendChild(link);
        list.appendChild(listItem);
    });
}
