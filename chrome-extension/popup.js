document.getElementById('searchForm').addEventListener('submit', function(event) {
  console.log("Form submitted...")
  event.preventDefault(); // Prevent the form from submitting normally
  const address = document.getElementById('searchQuery').value;
  fetchEstablishments(address)
});
  
function displayEstablishments(fetchEstablishmentsResponse){
    // Supermarkets
    displayEstablishmentsHandler(fetchEstablishmentsResponse, "Supermarkets", "closest-supermarket")

    // Gyms
    displayEstablishmentsHandler(fetchEstablishmentsResponse, "Gyms", "closest-fitness_centre")

    // Cafes
    displayEstablishmentsHandler(fetchEstablishmentsResponse, "Coffee Shops", "closest-cafe")

    // Schools
    displayEstablishmentsHandler(fetchEstablishmentsResponse, "Schools", "closest-school")

    // Parks
    displayEstablishmentsHandler(fetchEstablishmentsResponse, "Parks", "closest-park")

    // Banks
    displayEstablishmentsHandler(fetchEstablishmentsResponse, "Banks", "closest-bank")
}
  
function displayEstablishmentsHandler(fetchEstablishmentsResponse, headerName, responseNode) {
    
    // Updating address
    const address = `Apartment Address: ${fetchEstablishmentsResponse["received-address"]["content"]}`
    console.log("Address from displaySupermarkets in popup.js: " + address)
    document.getElementById('address').textContent = address
    
    // Creating section header
    var sectionHeader = document.createElement("h2");
    sectionHeader.textContent = headerName;
    var sectionHeaderId = "section-header-" + headerName.toLowerCase()
    sectionHeader.id = sectionHeaderId;

    // Adding section header to display div
    var displayDiv = document.getElementById("display");
    displayDiv.appendChild(sectionHeader);


    // Create an ordered list to display the establishments
    const list = document.createElement('ol');
    displayDiv.appendChild(list);


    // Access the array of supermarket elements
    const establishments = fetchEstablishmentsResponse[responseNode];

    // Slice the first three supermarkets if you only want to display a subset
    const firstThree = establishments.slice(0, 5);

    // Iterate over the supermarkets
    firstThree.forEach(establishment => {
        // Extract the necessary information
        const name = establishment.name;
        const lat = establishment.lat;
        const lon = establishment.lon;
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


function fetchEstablishments(address) {
  // Use `fetch` to send the address to your API and get the list of supermarkets
  // Then, store it using chrome.storage or send it directly to your popup
  
  console.log("fetchSupermarkets called...")
  
  fetch('https://real-estate-assistant-7c6723789c55.herokuapp.com/fetch-address-data', {
          method: 'POST',
          headers: {
              'Content-Type': 'application/json'
          },
          body: JSON.stringify({ content: address })
      })
      .then(response => response.json())
      .then(data => {
          console.log(data)
          console.log("Successfully received /fetch-address-data response!");
          console.log("Data from /fetch-address-data: " + data);
          
      chrome.storage.local.set({ fetchEstablishmentsResponse: data }, function() {
          console.log('Supermarkets are saved in chrome.storage.local');
      });
      
      displayEstablishments(data)
  })
  .catch(error => {
      console.error('Error fetching establishments data:', error);
  });
}