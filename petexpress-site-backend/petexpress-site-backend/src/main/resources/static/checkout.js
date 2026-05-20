document.addEventListener("DOMContentLoaded", () => {
    const button = document.getElementById("finalizar-compra");
    const apiBaseUrl = window.PETEXPRESS_API_URL || "http://localhost:8082";

    function getAuthHeaders() {
        try {
            const loggedUser = JSON.parse(localStorage.getItem("usuarioLogado") || "null");
            return loggedUser?.token ? { Authorization: `Bearer ${loggedUser.token}` } : {};
        } catch (error) {
            return {};
        }
    }
    if (!button) {
        return;
    }

    button.addEventListener("click", async () => {
        const cartJson = localStorage.getItem("cart");
        const cart = cartJson ? JSON.parse(cartJson) : [];

        if (!Array.isArray(cart) || cart.length === 0) {
            alert("O carrinho está vazio.");
            return;
        }

        const paymentItems = cart.map(item => ({
            title: item.nome || item.name || 'Produto',
            quantity: Number(item.quantidade || item.quantity || 1),
            price: Number(item.preco || item.price || 0)
        }));

        try {
            const response = await fetch(`${apiBaseUrl}/api/pagamento/criar-preferencia`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    ...getAuthHeaders()
                },
                body: JSON.stringify({
                    frontendBaseUrl: window.location.origin,
                    itens: paymentItems
                })
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => null);
                throw new Error(errorData?.message || `Erro ${response.status}`);
            }

            const data = await response.json();
            if (!data.init_point) {
                throw new Error("Resposta inválida do servidor.");
            }

            window.location.href = data.init_point;
        } catch (error) {
            console.error("Erro ao criar preferência de pagamento:", error);
            alert("Não foi possível iniciar o pagamento. Veja o console para mais detalhes.");
        }
    });
});

