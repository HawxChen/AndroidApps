#!/usr/bin/env python
from bottle import route, run, template, request, static_file, HTTPResponse
import matlab
import matlab.engine
import sqlite3
import os
from random import random
from hashlib import sha256
import pickle

def create_signature_from_signal_file(path):
    # Input file should contain samples
    #   - Captured at 160 Hz
    #   - 60s worth of data
    with open(path, 'rb') as f:
        data = [float(x) for x in f.readlines()]
    # Now run the feature extractor on the input data
    features = eng.FeatureExt(matlab.double(data))
    return features

def compare_signatures(sig1, sig2):
    # Compare two signatures
    # training_size = len(features) - 6
    training_size = len(sig1) / 12
    x = eng.Comparator(sig1, sig2, training_size)
    print 'Comparator returned', x
    return x

class User(object):
    def __init__(self, _id, name, signature):
        self.id = _id
        self.name = name
        self.signature = signature

class Database(object):
    def __init__(self):
        self._conn = sqlite3.connect('users.db')
        self._c = self._conn.cursor()

    def init_tables(self):
        """Initialize database tables if they do exist"""
        query = '''\
            CREATE TABLE IF NOT EXISTS `users` ( \
                `id`	INTEGER PRIMARY KEY AUTOINCREMENT, \
                `name`	TEXT NOT NULL, \
                `signature`	BLOB \
            )'''
        self._c.execute(query)
        self._conn.commit()

    def get_users(self, _id=None, name=None):
        """Get list of users"""
        query = '''\
            SELECT * FROM users \
            '''
        if _id is not None:
            query += "WHERE `id` = %d" % _id
        elif name is not None:
            query += "WHERE `name` = '%s' COLLATE NOCASE" % name
        users = []
        for uid, name, signature in self._c.execute(query):
            users.append(User(uid, name, pickle.loads(signature)))
        return users

    def add_user_to_database(self, user):
        """Adds user to database"""
        query = '''\
            INSERT INTO `users` (`name`, `signature`) VALUES (?, ?) \
            '''
        vals = (user.name, pickle.dumps(user.signature))
        self._c.execute(query, vals)
        self._conn.commit()
        user.id = self._c.lastrowid
        return user

    def remove_user(self, user):
        query = '''\
            DELETE FROM `users` WHERE `id` = ? \
            '''
        self._c.execute(query, (user.id,))
        self._conn.commit()
        return user

#
# WEB INTERFACE
#

@route('/static/<filename>')
def server_static(filename):
    return static_file(filename, root='static')

@route('/', method='GET')
def index():
    """Index page"""
    return template('templates/index')

@route('/admin')
def admin():
    """Admin page"""
    return template('templates/admin', users=db.get_users())

@route('/admin/create', method='POST')
def admin_create():
    """Create a new user"""
    # Get inputs from form data
    username = request.forms.get('username')

    # Save user signal data to a file
    signal = request.files.get('signal')
    upload_path = 'upload_' + sha256(str(random())).hexdigest()
    signal.save(upload_path)

    # Construct signature from user signal data
    signature = create_signature_from_signal_file(upload_path)

    # Create the user object and save user to database
    user = User(-1, username, signature)
    db.add_user_to_database(user)

    return '<a href="/admin">OK</a>'

@route('/admin/delete')
def admin_delete():
    """Delete a user"""
    uid = int(request.query['id'])
    users = db.get_users(_id=uid)
    if len(users) < 1:
        return "User does not exist"
    db.remove_user(users[0])
    return "OK"

@route('/login', method='GET')
def login():
    """Login page"""
    return template('templates/login')

@route('/login', method='POST')
def login_post():

    
    """Login POST handler"""
    # Get inputs from form data
    username = request.forms.get('username')

    # Save user signal data to a file
    signal = request.files.get('signal')
    upload_path = 'upload_' + sha256(str(random())).hexdigest()
    signal.save(upload_path)

    # Construct signature from user signal data
    signature = create_signature_from_signal_file(upload_path)

    # Find the user in the database
    users = db.get_users(name=username)
    if len(users) < 1:
        return "User does not exist"

    # Compare the signatures
    result = compare_signatures(users[0].signature, signature)
    if result == 0:
        return HTTPResponse(status=401, body="Invalid Signature")
    else:
        return HTTPResponse(status=200, body="Success")

#
# Main Function (runs everything)
#

def main():
    # Initialize database connection
    print 'Initializing database...'
    global db
    db = Database()
    db.init_tables()

    # Start MATLAB interface
    print 'Starting MATLAB interface (~10 seconds)...'
    global eng
    eng = matlab.engine.start_matlab()

    # Start the webserver
    print 'Starting server...'
    run(host='0.0.0.0', port=8080)

if __name__ == '__main__':
    main()
