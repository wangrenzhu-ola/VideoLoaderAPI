//
//  TestRoomCollectionViewCell.swift
//  TestVideoLoader
//
//  Created by wushengtao on 2023/8/30.
//

import UIKit

class TestRoomCollectionViewCell: UICollectionViewCell {
    public var broadcasterCount: Int = 1 {
        didSet {
            setNeedsLayout()
        }
    }
    public lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.textColor = .blue
        label.numberOfLines = 2
        label.textAlignment = .right
        label.font = UIFont.boldSystemFont(ofSize: 16)
        return label
    }()
    
    public lazy var mainBroadcasterView = UIView()
    public lazy var otherBroadcasterView = UIView()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        _loadSubview()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func _loadSubview() {
        mainBroadcasterView.backgroundColor = .yellow
        contentView.addSubview(otherBroadcasterView)
        contentView.addSubview(mainBroadcasterView)
        addSubview(titleLabel)
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        let canvasSize = CGSize(width: frame.width / 2, height: frame.width * 1.5 / 2)
        if broadcasterCount == 2 {
            otherBroadcasterView.isHidden = false
            mainBroadcasterView.frame = CGRect(origin: CGPoint(x: 0, y: (frame.height - canvasSize.height) / 2), size: canvasSize)
        } else {
            otherBroadcasterView.isHidden = true
            mainBroadcasterView.frame = bounds
        }
        otherBroadcasterView.frame = CGRect(origin: CGPoint(x: canvasSize.width, y: (frame.height - canvasSize.height) / 2), size: canvasSize)
        titleLabel.frame = bounds
        titleLabel.sizeToFit()
        titleLabel.frame = CGRect(x: frame.width - titleLabel.frame.width - 10, y: 80, width: titleLabel.bounds.width, height: titleLabel.bounds.height)
    }
}
