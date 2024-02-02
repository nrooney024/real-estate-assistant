function retryGetAddress(retries, delay) {
  return new Promise((resolve, reject) => {
    function attempt() {
      getAddressFromPage()
        .then(address => {
          if (address.length > 0) {
            resolve(address); // Address found, resolve the promise
          } else if (retries === 0) {
            reject(new Error('No address found after retries')); // No more retries, reject the promise
          } else {
            console.log(`Retrying... Attempts left: ${retries}`);
            retries--;
            setTimeout(attempt, delay); // Wait for 'delay' ms before retrying
          }
        })
        .catch(reject); // Propagate errors from getAddressFromPage
    }

    attempt();
  });
}

chrome.runtime.onMessage.addListener(
  function(request, sender, sendResponse) {
    if (request.message === "get_address") {
      console.log("Content script received get_address message...");
      retryGetAddress(3, 2000).then(address => {
        sendResponse({address: address});
        console.log("Content script sent the address to the background script...");
      }).catch(error => {
        console.error('Error in retryGetAddress:', error);
        sendResponse({address: null}); // Send null or an appropriate error response.
      });
      return true; // Indicates you wish to send a response asynchronously.
    }
  }
);

// Assuming getAddressFromPage might be asynchronous.
function getAddressFromPage() {
  console.log("getAddressFromPage started...");
  // Your existing logic to scrape the address...
  // Just ensure that it returns a Promise if it's asynchronous.

  // For instance, if it involves a fetch or any async operation, it should be something like this:
  return new Promise((resolve, reject) => {
    try {
      const contentElements = document.querySelectorAll("div.summary-container h1");
      const address = [];
      contentElements.forEach(element => {
        address.push(element.textContent.trim()); // Added trim() to clean the text
      });
      console.log("Address scraped:", address);
      resolve(address); // Resolves the promise with the address
    } catch (error) {
      reject(error); // Rejects the promise if there's an error
    }
  });
}