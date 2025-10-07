document.addEventListener("DOMContentLoaded", () => {
    const frameElement = document.getElementById("processedFrame") as HTMLImageElement;
    const statsElement = document.getElementById("stats") as HTMLDivElement;

    // PASTE the very long Base64 string you copied from Logcat here
    const base64ImageString = "iVBORw0KGgoAAAANSUhEUgA...your...long...string...here...=";

    if (frameElement) {
        // The 'data:image/jpeg;base64,' part is required by browsers
        frameElement.src = `data:image/jpeg;base64,${base64ImageString}`;
    }

    if (statsElement) {
        // Display sample frame stats as required [cite: 39]
        statsElement.innerHTML = `
            <h3>Frame Stats</h3>
            <p><strong>Resolution:</strong> 640x480</p>
            <p><strong>FPS (Android):</strong> ~15 FPS</p>
        `;
    }
});