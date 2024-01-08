chrome.browserAction.onClicked.addListener(function(tab) {
    chrome.runtime.reload();
});

chrome.tabs.onUpdated.addListener(function(tabId, changeInfo, tab) {
    if (changeInfo.status == 'complete') {
        alert("tab updated");
    }
});