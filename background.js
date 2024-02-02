chrome.webNavigation.onHistoryStateUpdated.addListener(function(details) {
  console.log("webNavigation.onHistoryStateUpdated triggered...")
    // Check if the tab is on Zillow
    if (details.url.includes("zillow.com")) {
      chrome.tabs.sendMessage(details.tabId, {message: "get_address"}, function(response) {
        console.log("Content script (chrome.webNavigation.onHistoryStateUpdated) get_address response: ", response)
        if(response.address) {
            console.log("Background script received address from content script...")
            fetchSupermarkets(response.address);
            console.log("Background script called address data fetch...")
        }
      });
    }
});


  
  function fetchSupermarkets(address) {
    // Use `fetch` to send the address to your API and get the list of supermarkets
    // Then, store it using chrome.storage or send it directly to your popup
    
    console.log("fetchSupermarkets called...")
    
    fetch('http://localhost:8080/fetch-address-data', {
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
            
        // Option 1: Store the data for access by the popup later.
        chrome.storage.local.set({ supermarkets: data }, function() {
            console.log('Supermarkets are saved in chrome.storage.local');
        });
        
    })
    .catch(error => {
        console.error('Error fetching supermarket data:', error);
    });
  }
  



