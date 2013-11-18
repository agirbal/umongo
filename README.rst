=======================
UMONGO, the MongoDB GUI
=======================

.. header:: UMONGO, the MongoDB GUI
.. footer:: Copyright (C) 2010 EdgyTech LLC.

About
-----

This version of UMongo is provided as free software by EdgyTech LLC.
UMongo is open source, and sources can be seen at https://github.com/agirbal/umongo .
The libraries used by UMongo either have an open source license, or are proprietary and owned by EdgyTech LLC.

Download
--------

All binaries are available from:
http://www.edgytech.com/umongo

Install and Run
---------------

Windows
^^^^^^^

Steps:

- place archive file where is preferred (for example, in your downloads folder)
- extract the archive (right-click “Extract All”)
- open the extracted folder and double-click umongo (type Application)

Linux
^^^^^

Steps:

- place archive file where is preferred (for example, in your downloads folder)
- extract the archive (right-click “Extract Here”)
- open the extracted folder and double-click launch-umongo.sh, or execute launch-umongo.sh in a terminal

Mac OSX
^^^^^^^

**If your Mac tells you that the application is damaged**, go to "system preferences / security and privacy / General" and make sure that "allow applications downloaded from" is set to "anywhere".

Steps:

- place archive file where is preferred and extract it (for example, in your downloads folder)
- extract the archive (double-click the file)
- open the extracted folder and double-click umongo.app

Command line
^^^^^^^^^^^^

On any platform you can run UMongo from the command line.
Open a terminal and go to UMongo's directory in which umongo.jar is present and run::

    > java -jar umongo.jar

Using a Proxy
-------------

Often times your client machine cannot directly connect to the MongoDB hosts.
One easy trick is to use a ``SOCKS`` proxy based on SSH tunnel to connect transparently::

    > ssh -D 9000 -i key.pem hostThatCanConnectToDB -N

Then make sure you select a ``SOCKS`` proxy of "localhost:9000" in the connection settings.

Building UMongo
---------------

To compile:

-   go to the base directory and run::

    > ant jar

To package the application:

-   go to the ``package`` folder and run::

    > ./package.sh

To set the version:

- change the version number in ``manifest.mf`` prior to compile / package


