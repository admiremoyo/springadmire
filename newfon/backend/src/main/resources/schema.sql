-- Schema for `ourusers` table
CREATE TABLE ourusers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    fileNumber VARCHAR(50),
    standNumber VARCHAR(50),
    yearOfPurchase VARCHAR(50) NOT NULL,  -- Ensure casing consistency
    director VARCHAR(255),
    squareMetres VARCHAR(50),
    descriptionOfStand TEXT NOT NULL,  -- Ensure casing consistency
    cellNumber VARCHAR(50),
    standPrice DECIMAL(15, 2) DEFAULT 0.00,

);

-- Schema for `payments` table
CREATE TABLE payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    paymentDate DATE NOT NULL,
    receiptNumber VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES ourusers(id) ON DELETE CASCADE
);
