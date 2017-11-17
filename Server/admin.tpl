<h1>Admin Interface</h1>

<hr />

<h2>Users List</h2>
<table border="1">
	<tr>
		<th>ID</th>
		<th>Name</th>
		<th>Signature</th>
		<th>Actions</th>
	</tr>
% for user in users:
	<tr>
		<td>{{ user.id }}</td>
		<td>{{ user.name }}</td>
		<td>{{ user.signature }}</td>
		<td><a href="/admin/delete?id={{ user.id }}">Delete User</a></td>
	</tr>
% end
</table>

<hr />

<h2>Create New User</h2>
<form method="post" action="/admin/create" enctype="multipart/form-data">
<table border="1">
	<tr>
		<th>User Name</th>
		<td><input type="text" name="username" /></td>
	</tr>
	<tr>
		<th>Signal Data File</th>
		<td><input type="file" name="signal_upload" /></td>
	</tr>
	<tr>
		<td colspan="2"><input type="submit" name="Create User"></td>
	</tr>
</table>
</form>
