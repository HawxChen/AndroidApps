#!/usr/bin/env python
from bottle import route, run, template, request
import matlab
import matlab.engine
import sqlite3
import os
from random import random
from hashlib import sha256

#print 'Starting MATLAB interface (~10 seconds)...'
#eng = matlab.engine.start_matlab()

conn = sqlite3.connect('users.db')
c = conn.cursor()

def init_tables():
	"""Initialize database tables if they do exist"""
	query = '''\
		CREATE TABLE IF NOT EXISTS `users` ( \
			`id`	INTEGER PRIMARY KEY AUTOINCREMENT, \
			`name`	TEXT NOT NULL, \
			`signature`	BLOB \
		)'''
	c.execute(query)
	conn.commit()

def main():
    features = eng.FeatureExt(matlab.double(data))
    print 'Feature extraction successful...'
    print features

    print 'Length = ', len(features)

    print 'Trying comparator'
    signature = features
    signal_alpha = features
    # training_size = len(features) - 6
    training_size = len(features) / 6
    x = eng.Comparator(signature, signal_alpha, training_size)
    print 'Comparator returned', x

@route('/hello/<name>')
def index(name):
    return template('<b>Hello {{name}}</b>!', name=name)

class User(object):
	def __init__(self, _id, name, signature):
		self.id = _id
		self.name = name
		self.signature = signature

@route('/admin')
def admin():
	query = '''\
		SELECT * FROM users \
		'''
	users = []
	for uid, name, signature in c.execute(query):
		users.append(User(uid, name, "..."))
	return template('admin', users=users)

@route('/admin/create', method='POST')
def admin_create():
	"""Create a new user"""
	username = request.forms.getall('username')
	signal = request.files.get('signal')
	upload_path = 'upload_' + sha256(str(random())).hexdigest()
	signal.save(upload_path)
	return 'OK'

@route('/login')
def login():
	return template('login')

init_tables()
run(host='localhost', port=8080)
