<!doctype html>
<html lang="en">
<head>
	<title>Login</title>
	<meta charset="utf-8">
	<link rel="stylesheet" href="/static/bootstrap.min.css">
	<link rel="stylesheet" href="/static/style.css">
</head>
<body>
<div class="container">

<form method="post" action="/login" enctype="multipart/form-data" class="form-login">
	<h2>Login</h2>
	<div class="form-group">
		<label for="username">Username</label>
		<input type="text" class="form-control" id="username" name="username" placeholder="Username">
	</div>
	<div class="form-group">
		<label for="signal">User Signal File</label>
		<input type="file" class="form-control-file" id="signal" name="signal" aria-describedby="fileHelp">
		<small id="fileHelp" class="form-text text-muted">This file should contain the test signal of the user and will be used for authentication against the known signal.</small>
	</div>
	<input type="submit" class="btn btn-lg btn-primary btn-block" name="Login">
</form>

</div>
</body>
</html>