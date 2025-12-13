document.addEventListener("DOMContentLoaded", () => {
    const medicineList = document.getElementById("medicine-list");

    const medicines = [
        {name:"Paracetamol", price:20},
        {name:"Napa Extra", price:25},
        {name:"Vitamin C", price:15},
        {name:"Antacid", price:10}
    ];

    medicines.forEach(med => {
        const div = document.createElement("div");
        div.className = "card";
        div.innerHTML = `
            <h3>${med.name}</h3>
            <p>Price: $${med.price}</p>
            <button>Add to Cart</button>
        `;
        medicineList.appendChild(div);
    });
});