#vim: ft=ruby
Vagrant.configure(2) do |config|
  config.vm.box = "ubuntu/trusty64"

  config.vm.provider "virtualbox" do |vb|
    vb.memory = "2048"
    vb.cpus = 2
  end

  config.vm.provision "shell", privileged: false, inline: <<-SHELL
    # install some packages
    #sudo apt-get update
    sudo apt-get install -y git

    # manually install a more recent golang
    wget --quiet https://storage.googleapis.com/golang/go1.5.3.linux-amd64.tar.gz
    sudo tar -C /usr/local -xzf go1.5.3.linux-amd64.tar.gz
    rm go1.5.3.linux-amd64.tar.gz

    # create our golang environment, and set GOPATH/PATH
    if [ ! -d ~/go ]; then
      mkdir -p ~/go/src/github.com/abtrout ~/go/bin ~/go/pkg
      ln -sf /vagrant ~/go/src/github.com/abtrout/project
      ln -sf /vagrant ~/work

      grep "GOPATH" ~/.profile > /dev/null
      if [ $? -ne 0 ]; then
        echo "export GOPATH=$HOME/go" >> ~/.profile
        echo "export PATH=$PATH:/usr/local/go/bin:$GOPATH/bin" >> ~/.profile
        source ~/.profile
      fi
    fi

    go get github.com/shopify/sarama
    go get github.com/julienschmidt/httprouter
    go get github.com/tsuna/gohbase

    # TODO: add scala installation here, too
  SHELL
end
