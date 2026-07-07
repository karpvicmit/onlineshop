const stripe = Stripe(stripePublishableKey);

let elements;

initialize();

async function initialize() {
    try {
        const response = await fetch('/api/payments/create', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ orderId: orderId })
        });

        if (!response.ok) {
            throw new Error('Failed to create payment');
        }

        const { sessionId } = await response.json();

        elements = stripe.elements({ clientSecret: sessionId });

        const paymentElementOptions = {
            layout: "tabs",
        };

        const paymentElement = elements.create("payment", paymentElementOptions);
        paymentElement.mount("#payment-element");

    } catch (error) {
        showMessage('Fehler beim Initialisieren der Zahlung: ' + error.message);
    }
}

async function handleSubmit(e) {
    e.preventDefault();
    setLoading(true);

    const { error } = await stripe.confirmPayment({
        elements,
        confirmParams: {
            return_url: window.location.origin + "/shop/orders/" + orderId,
        },
    });

    if (error) {
        if (error.type === "card_error" || error.type === "validation_error") {
            showMessage(error.message);
        } else {
            showMessage("Ein unerwarteter Fehler ist aufgetreten.");
        }
    }

    setLoading(false);
}

const paymentForm = document.getElementById("payment-form");
paymentForm.addEventListener("submit", handleSubmit);

function showMessage(messageText) {
    const messageContainer = document.querySelector("#payment-message");
    messageContainer.classList.remove("hidden");
    messageContainer.textContent = messageText;
}

function setLoading(isLoading) {
    const submitButton = document.querySelector("#submit");
    const spinner = document.querySelector("#spinner");
    const buttonText = document.querySelector("#button-text");

    submitButton.disabled = isLoading;
    spinner.classList.toggle("hidden", !isLoading);
    buttonText.classList.toggle("hidden", isLoading);
}