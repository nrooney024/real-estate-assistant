document.addEventListener('DOMContentLoaded', function() {
    // When the popup DOM is fully loaded, retrieve the supermarket data.
    chrome.storage.local.get(['supermarkets'], function(result) {
      if(result.supermarkets) {
        // If supermarkets are found in storage, display them.
        console.log("result.supermarkets from popup.js chrome storage: ", result.supermarkets)
        displaySupermarkets(result.supermarkets);
      }
    });
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
