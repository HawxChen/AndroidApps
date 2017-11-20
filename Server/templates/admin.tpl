<!doctype html>
<html lang="en">
<head>
	<title>Admin</title>
	<meta charset="utf-8">
	<link rel="stylesheet" href="/static/bootstrap.min.css">
	<link rel="stylesheet" href="/static/style.css">
</head>
<body>
<div class="container">

<h1>Admin Interface</h1>

<hr><a href="/login">Test Login Interface</a><hr>

<h2>Users</h2>
<table class="user-list table table-striped">
	<tr>
		<th>ID</th>
		<th>Name</th>
		<th style="width: 100%">Signature</th>
		<th>Actions</th>
	</tr>
	% for user in users:
	<tr>
		<td>{{ user.id }}</td>
		<td>{{ user.name }}</td>
		<td><div class="scrollable">{{ user.signature }}</div></td>
		<td><a href="/admin/delete?id={{ user.id }}">Delete User</a></td>
	</tr>
	% end
</table>

<hr>

<h2>Create New User</h2>
<form method="post" action="/admin/create" enctype="multipart/form-data" class="form-create-user">
<div class="form-group">
	<label for="username">Username</label>
	<input type="text" class="form-control" id="username" name="username" placeholder="Username">
</div>
<div class="form-group">
	<label for="signal">User Signal File</label>
	<input type="file" class="form-control-file" id="signal" name="signal" aria-describedby="fileHelp">
	<small id="fileHelp" class="form-text text-muted">This file should contain the base signal of the user and will be used for future authentication.</small>
</div>
<input type="submit" class="btn btn-lg btn-primary btn-block" name="Create User">
</form>

</div>
</body>
</html>
