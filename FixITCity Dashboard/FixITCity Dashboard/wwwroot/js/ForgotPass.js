let seconds = 5;

const countdown =
    document.getElementById("countdown");


const timer =
    setInterval(function () {

        seconds--;

        countdown.textContent = seconds;


        if (seconds <= 0) {

            clearInterval(timer);

            window.location.href = "/Login";
        }

    }, 1000);