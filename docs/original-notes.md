POST   /api/auth/register
POST   /api/auth/login

GET    /api/users/{id}
PUT    /api/users/{id}

GET    /api/posts
POST   /api/posts
GET    /api/posts/{id}
PUT    /api/posts/{id}
DELETE /api/posts/{id}

POST   /api/posts/{id}/comments
GET    /api/posts/{id}/comments

POST   /api/users/{id}/block
DELETE /api/users/{id}/block


---------------------------------------

Mappers
mapTo => UserEntity -> UserDto
mapFrom => UserDto -> UserEntity

--------------------------------------

RegisterRequest
├── username
├── password
└── email


UserDto
├── id
├── username
├── email
├── role
├── createdAt
└── accessedAt

The password only enters your system through the registration/login request.
It doesn't come back out through UserDto