-- ===============================
-- USERS
-- ===============================
CREATE TABLE IF NOT EXISTS users
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY
    PRIMARY
    KEY,
    name
    VARCHAR
(
    255
) NOT NULL,
    email VARCHAR
(
    512
) NOT NULL UNIQUE
    );

-- ===============================
-- REQUESTS
-- ===============================
CREATE TABLE IF NOT EXISTS requests
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY
    PRIMARY
    KEY,
    description
    VARCHAR
(
    1024
) NOT NULL,
    requestor_id BIGINT NOT NULL,
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_requests_requestor
    FOREIGN KEY
(
    requestor_id
)
    REFERENCES users
(
    id
)
    );

-- ===============================
-- ITEMS
-- ===============================
CREATE TABLE IF NOT EXISTS items
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY
    PRIMARY
    KEY,
    name
    VARCHAR
(
    255
) NOT NULL,
    description VARCHAR
(
    1024
) NOT NULL,
    is_available BOOLEAN NOT NULL,
    owner_id BIGINT NOT NULL,
    request_id BIGINT,
    CONSTRAINT fk_items_owner
    FOREIGN KEY
(
    owner_id
)
    REFERENCES users
(
    id
),
    CONSTRAINT fk_items_request
    FOREIGN KEY
(
    request_id
)
    REFERENCES requests
(
    id
)
    );

-- ===============================
-- BOOKINGS
-- ===============================
CREATE TABLE IF NOT EXISTS bookings
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY
    PRIMARY
    KEY,
    start_date
    TIMESTAMP
    WITHOUT
    TIME
    ZONE
    NOT
    NULL,
    end_date
    TIMESTAMP
    WITHOUT
    TIME
    ZONE
    NOT
    NULL,
    item_id
    BIGINT
    NOT
    NULL,
    booker_id
    BIGINT
    NOT
    NULL,
    status
    VARCHAR
(
    32
) NOT NULL,
    CONSTRAINT fk_bookings_item
    FOREIGN KEY
(
    item_id
)
    REFERENCES items
(
    id
),
    CONSTRAINT fk_bookings_booker
    FOREIGN KEY
(
    booker_id
)
    REFERENCES users
(
    id
)
    );

-- ===============================
-- COMMENTS
-- ===============================
CREATE TABLE IF NOT EXISTS comments
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY
    PRIMARY
    KEY,
    text
    VARCHAR
(
    1024
) NOT NULL,
    item_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_comments_item
    FOREIGN KEY
(
    item_id
)
    REFERENCES items
(
    id
),
    CONSTRAINT fk_comments_author
    FOREIGN KEY
(
    author_id
)
    REFERENCES users
(
    id
)
    );
