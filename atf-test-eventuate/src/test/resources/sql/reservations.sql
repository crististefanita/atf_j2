CREATE TABLE atf_order_reservations (
    reservation_id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount DECIMAL(30, 2) NOT NULL,
    state VARCHAR(32) NOT NULL
);
